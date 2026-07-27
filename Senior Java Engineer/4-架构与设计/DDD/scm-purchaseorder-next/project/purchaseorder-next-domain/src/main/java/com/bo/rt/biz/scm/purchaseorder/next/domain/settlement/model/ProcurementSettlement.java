package com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.event.SettlementEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.model.SettlementRevision.FeeDirection;
import com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.model.SettlementRevision.FeeItem;
import com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.model.SettlementRevision.SettlementBasis;
import com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.model.SettlementRevision.SettlementLine;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.AbstractAggregateRoot;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Money;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 采购结算聚合，管理当前有效结算版本及向 AP 提交的状态。
 */
public class ProcurementSettlement extends AbstractAggregateRoot {

    /** 结算聚合标识。 */
    private final String id;
    /** 结算所归属的采购订单标识。 */
    private final String purchaseOrderId;
    /** 应收款供应商编码。 */
    private final String supplierCode;
    /** 应付款采购主体标识。 */
    private final String buyerOrganizationId;
    /** 本次结算统一使用的币种。 */
    private final String currency;
    /** 各采购订单行当前生效的含税单价快照。 */
    private final Map<String, Money> linePrices;
    /** 按采购订单行累计的可结算验收数量。 */
    private final Map<String, Quantity> acceptedQuantities = new HashMap<>();
    /** 已入账验收事实标识，用于阻止消息重放造成重复结算。 */
    private final Set<String> acceptanceFactIds = new HashSet<>();
    /** 已入账的运费、扣款等费用事实。 */
    private final List<FeeItem> feeFacts = new ArrayList<>();
    /** 当前价格快照对应的采购订单版本。 */
    private long purchaseOrderVersion;
    /** 结算处理状态。 */
    private SettlementStatus status;
    /** 当前生效的不可变计算版本，尚未计算时为空。 */
    private SettlementRevision currentRevision;
    /** 已生成的最大结算版本号，只增不减。 */
    private int latestRevisionNo;
    /** AP 接受结算后返回的应付单据号。 */
    private String apDocumentNo;

    /**
     * 根据已生效采购订单的商业快照创建结算聚合。
     */
    private ProcurementSettlement(
            String id,
            String purchaseOrderId,
            String supplierCode,
            String buyerOrganizationId,
            String currency,
            long purchaseOrderVersion,
            Map<String, Money> linePrices
    ) {
        require(id != null && !id.isBlank(), "结算 ID 不能为空");
        require(purchaseOrderId != null && !purchaseOrderId.isBlank(), "采购订单 ID 不能为空");
        require(supplierCode != null && !supplierCode.isBlank(), "供应商不能为空");
        require(
                buyerOrganizationId != null && !buyerOrganizationId.isBlank(),
                "采购主体不能为空"
        );
        require(currency != null && !currency.isBlank(), "币种不能为空");
        require(purchaseOrderVersion > 0, "采购订单版本必须大于零");
        require(linePrices != null && !linePrices.isEmpty(), "订单行价格快照不能为空");
        require(
                linePrices.values().stream().allMatch(price -> currency.equals(price.currency())),
                "订单行价格币种不一致"
        );
        this.id = id;
        this.purchaseOrderId = purchaseOrderId;
        this.supplierCode = supplierCode;
        this.buyerOrganizationId = buyerOrganizationId;
        this.currency = currency;
        this.purchaseOrderVersion = purchaseOrderVersion;
        this.linePrices = new HashMap<>(linePrices);
        this.status = SettlementStatus.WAITING_FOR_BASIS;
        long eventVersion = nextVersion();
        raise(new SettlementEvents.ProcurementSettlementInitialized(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                supplierCode,
                currency
        ));
    }

    /**
     * 初始化采购订单对应的结算台账。
     *
     * @return 等待验收与费用依据的结算聚合
     */
    public static ProcurementSettlement initialize(
            String id,
            String purchaseOrderId,
            String supplierCode,
            String buyerOrganizationId,
            String currency,
            long purchaseOrderVersion,
            Map<String, Money> linePrices
    ) {
        return new ProcurementSettlement(
                id,
                purchaseOrderId,
                supplierCode,
                buyerOrganizationId,
                currency,
                purchaseOrderVersion,
                linePrices
        );
    }

    /**
     * 将一条验收事实计入可结算数量。
     *
     * <p>同一事实只能入账一次；已有计算版本时，本操作会要求重新计算。</p>
     */
    public void applyAcceptedQuantity(
            String acceptanceFactId,
            String purchaseOrderLineId,
            Quantity quantity
    ) {
        require(acceptanceFactId != null && !acceptanceFactId.isBlank(), "验收事实 ID 不能为空");
        require(!acceptanceFactIds.contains(acceptanceFactId), "同一验收事实不能重复入账");
        require(linePrices.containsKey(purchaseOrderLineId), "验收事实引用了未知订单行");
        require(quantity != null && quantity.isPositive(), "可结算数量必须大于零");
        acceptedQuantities.merge(purchaseOrderLineId, quantity, Quantity::add);
        acceptanceFactIds.add(acceptanceFactId);
        markRecalculationRequired("收到新的可结算数量");
    }

    /**
     * 应用采购订单的新价格快照。
     *
     * <p>只有更高的采购订单版本可以覆盖现有价格，防止乱序事件回滚结算依据。</p>
     */
    public void applyPriceAdjustment(
            String purchaseOrderLineId,
            Money newTaxedUnitPrice,
            long newPurchaseOrderVersion
    ) {
        require(linePrices.containsKey(purchaseOrderLineId), "调价引用了未知订单行");
        require(
                newTaxedUnitPrice != null && currency.equals(newTaxedUnitPrice.currency()),
                "调价币种不一致"
        );
        require(newPurchaseOrderVersion > purchaseOrderVersion, "调价事件版本必须更新");
        linePrices.put(purchaseOrderLineId, newTaxedUnitPrice);
        purchaseOrderVersion = newPurchaseOrderVersion;
        markRecalculationRequired("采购订单价格发生变化");
    }

    /**
     * 将一条费用事实计入结算依据。
     *
     * <p>费用事实按 {@link FeeItem#factId()} 幂等；已有计算版本时需重新计算。</p>
     */
    public void applyFeeFact(FeeItem feeItem) {
        require(feeItem != null, "费用事实不能为空");
        require(currency.equals(feeItem.amount().currency()), "费用币种不一致");
        require(
                feeFacts.stream().noneMatch(existing -> existing.factId().equals(feeItem.factId())),
                "同一费用事实不能重复入账"
        );
        feeFacts.add(feeItem);
        markRecalculationRequired("收到新的费用事实");
    }

    /**
     * 根据当前验收数量、价格、税务规则和费用事实生成新的不可变结算版本。
     *
     * <p>重新计算不会覆盖历史版本，而是递增版本号并产生新的当前版本。</p>
     */
    public void recalculate(
            BigDecimal taxRate,
            String taxRuleVersion,
            String feeRuleVersion
    ) {
        require(!acceptedQuantities.isEmpty(), "尚无可结算数量");
        require(taxRate != null && taxRate.signum() >= 0, "税率不能为负数");
        require(
                status == SettlementStatus.WAITING_FOR_BASIS
                        || status == SettlementStatus.RECALCULATION_REQUIRED
                        || status == SettlementStatus.AP_REJECTED
                        || status == SettlementStatus.CALCULATED,
                "当前状态不能重新计算"
        );

        BigDecimal divisor = BigDecimal.ONE.add(taxRate);
        List<SettlementLine> lines = acceptedQuantities.entrySet().stream()
                .map(entry -> {
                    Money taxedUnitPrice = linePrices.get(entry.getKey());
                    Money taxedAmount = taxedUnitPrice.multiply(entry.getValue());
                    Money netAmount = new Money(
                            taxedAmount.amount().divide(divisor, 2, RoundingMode.HALF_UP),
                            currency
                    );
                    Money taxAmount = taxedAmount.subtract(netAmount);
                    return new SettlementLine(
                            entry.getKey(),
                            entry.getValue(),
                            taxedUnitPrice,
                            netAmount,
                            taxAmount
                    );
                })
                .toList();

        Money goodsAmount = lines.stream()
                .map(SettlementLine::netAmount)
                .reduce(Money.zero(currency), Money::add);
        Money taxAmount = lines.stream()
                .map(SettlementLine::taxAmount)
                .reduce(Money.zero(currency), Money::add);
        Money feeAmount = feeFacts.stream()
                .map(fee -> fee.direction() == FeeDirection.ADD
                        ? fee.amount()
                        : new Money(fee.amount().amount().negate(), currency))
                .reduce(Money.zero(currency), Money::add);
        Money totalPayable = goodsAmount.add(taxAmount).add(feeAmount);
        require(totalPayable.amount().signum() >= 0, "应付金额不能为负数");

        currentRevision = new SettlementRevision(
                ++latestRevisionNo,
                new SettlementBasis(
                        purchaseOrderVersion,
                        Set.copyOf(acceptanceFactIds),
                        taxRuleVersion,
                        feeRuleVersion
                ),
                lines,
                List.copyOf(feeFacts),
                goodsAmount,
                taxAmount,
                totalPayable
        );
        status = SettlementStatus.CALCULATED;
        long eventVersion = nextVersion();
        raise(new SettlementEvents.SettlementCalculated(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                currentRevision.revisionNo(),
                currentRevision.totalPayable()
        ));
    }

    /**
     * 确认当前计算版本，冻结本次准备提交 AP 的金额。
     */
    public void confirm() {
        require(status == SettlementStatus.CALCULATED, "只有已计算版本可以确认");
        status = SettlementStatus.CONFIRMED;
        long eventVersion = nextVersion();
        raise(new SettlementEvents.SettlementConfirmed(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                currentRevision.revisionNo(),
                currentRevision.totalPayable()
        ));
    }

    /**
     * 将已确认版本标记为提交 AP，并返回供外部调用使用的幂等键。
     *
     * @return 由结算标识和版本号组成的 AP 提交幂等键
     */
    public String submitToAp() {
        require(status == SettlementStatus.CONFIRMED, "结算版本尚未确认");
        status = SettlementStatus.SUBMITTING_TO_AP;
        String idempotencyKey = id + ":" + currentRevision.revisionNo();
        long eventVersion = nextVersion();
        raise(new SettlementEvents.SettlementSubmittedToAp(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                currentRevision.revisionNo(),
                idempotencyKey
        ));
        return idempotencyKey;
    }

    /**
     * 记录 AP 已接受当前结算版本及其外部单据号。
     */
    public void recordApAcceptance(String apDocumentNo) {
        require(status == SettlementStatus.SUBMITTING_TO_AP, "结算未提交 AP");
        require(apDocumentNo != null && !apDocumentNo.isBlank(), "AP 单号不能为空");
        this.apDocumentNo = apDocumentNo;
        status = SettlementStatus.AP_ACCEPTED;
        long eventVersion = nextVersion();
        raise(new SettlementEvents.SettlementAcceptedByAp(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                currentRevision.revisionNo(),
                apDocumentNo
        ));
    }

    /**
     * 记录 AP 拒绝当前结算版本，使其可以修正依据后重新计算。
     */
    public void recordApRejection(String reason) {
        require(status == SettlementStatus.SUBMITTING_TO_AP, "结算未提交 AP");
        require(reason != null && !reason.isBlank(), "AP 拒绝原因不能为空");
        status = SettlementStatus.AP_REJECTED;
        long eventVersion = nextVersion();
        raise(new SettlementEvents.SettlementRejectedByAp(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                currentRevision.revisionNo(),
                reason
        ));
    }

    /**
     * 根据 AP 的付款完成事实将结算标记为已结清。
     */
    public void markSettled(String apDocumentNo) {
        require(status == SettlementStatus.AP_ACCEPTED, "AP 尚未接受结算");
        require(this.apDocumentNo.equals(apDocumentNo), "AP 单号不匹配");
        status = SettlementStatus.SETTLED;
        nextVersion();
    }

    /**
     * 作废尚未结清的结算；已付款结算必须通过冲销流程处理。
     */
    public void invalidate(String reason) {
        require(status != SettlementStatus.SETTLED, "已付款结算不能直接作废");
        require(reason != null && !reason.isBlank(), "作废原因不能为空");
        status = SettlementStatus.INVALIDATED;
        nextVersion();
    }

    /**
     * 在结算依据变化后推进状态；仅当已有计算版本时发布重算事件。
     */
    private void markRecalculationRequired(String reason) {
        if (currentRevision == null) {
            status = SettlementStatus.WAITING_FOR_BASIS;
            nextVersion();
            return;
        }
        status = SettlementStatus.RECALCULATION_REQUIRED;
        long eventVersion = nextVersion();
        raise(new SettlementEvents.SettlementRecalculationRequired(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                reason
        ));
    }

    /** 返回结算聚合标识。 */
    public String id() {
        return id;
    }

    /** 返回关联的采购订单标识。 */
    public String purchaseOrderId() {
        return purchaseOrderId;
    }

    /** 返回供应商编码。 */
    public String supplierCode() {
        return supplierCode;
    }

    /** 返回采购主体标识。 */
    public String buyerOrganizationId() {
        return buyerOrganizationId;
    }

    /** 返回当前结算状态。 */
    public SettlementStatus status() {
        return status;
    }

    /** 返回当前生效的结算版本，尚未计算时为空。 */
    public SettlementRevision currentRevision() {
        return currentRevision;
    }

    /** 采购结算从等待依据到结清或作废的生命周期状态。 */
    public enum SettlementStatus {
        WAITING_FOR_BASIS,
        CALCULATED,
        CONFIRMED,
        SUBMITTING_TO_AP,
        AP_ACCEPTED,
        SETTLED,
        RECALCULATION_REQUIRED,
        AP_REJECTED,
        INVALIDATED
    }
}
