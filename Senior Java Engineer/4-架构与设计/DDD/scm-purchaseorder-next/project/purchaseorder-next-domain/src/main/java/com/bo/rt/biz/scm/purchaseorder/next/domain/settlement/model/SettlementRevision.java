package com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Money;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import java.util.List;
import java.util.Set;

/**
 * 一次不可变结算计算结果。历史版本由查询仓储保存，不塞回聚合长期加载。
 *
 * @param revisionNo 结算版本号，在同一结算聚合内单调递增
 * @param basis 本版本采用的订单、验收、税务与费用规则依据
 * @param lines 按采购订单行计算的货款和税额
 * @param feeItems 本版本纳入计算的加项或扣项费用
 * @param goodsAmount 未税货款合计
 * @param taxAmount 税额合计
 * @param totalPayable 最终应付金额
 */
public record SettlementRevision(
        int revisionNo,
        SettlementBasis basis,
        List<SettlementLine> lines,
        List<FeeItem> feeItems,
        Money goodsAmount,
        Money taxAmount,
        Money totalPayable
) {

    /** 校验计算结果完整性并固化行与费用列表。 */
    public SettlementRevision {
        require(revisionNo > 0, "结算版本号必须大于零");
        require(basis != null, "结算依据不能为空");
        require(lines != null && !lines.isEmpty(), "结算版本至少包含一行");
        lines = List.copyOf(lines);
        feeItems = feeItems == null ? List.of() : List.copyOf(feeItems);
        require(goodsAmount != null && taxAmount != null && totalPayable != null, "结算金额不能为空");
    }

    /**
     * 可追溯的结算计算依据。
     *
     * @param purchaseOrderVersion 采用的采购订单商业版本
     * @param acceptanceFactIds 纳入计算的验收事实标识集合
     * @param taxRuleVersion 采用的税务规则版本
     * @param feeRuleVersion 采用的费用规则版本
     */
    public record SettlementBasis(
            long purchaseOrderVersion,
            Set<String> acceptanceFactIds,
            String taxRuleVersion,
            String feeRuleVersion
    ) {

        /** 校验并固化本版本采用的结算依据。 */
        public SettlementBasis {
            require(purchaseOrderVersion > 0, "采购订单版本必须大于零");
            require(acceptanceFactIds != null && !acceptanceFactIds.isEmpty(), "验收事实不能为空");
            acceptanceFactIds = Set.copyOf(acceptanceFactIds);
            require(taxRuleVersion != null && !taxRuleVersion.isBlank(), "税务规则版本不能为空");
            require(feeRuleVersion != null && !feeRuleVersion.isBlank(), "费用规则版本不能为空");
        }
    }

    /**
     * 一个采购订单行的结算计算结果。
     *
     * @param purchaseOrderLineId 采购订单行标识
     * @param settlementQuantity 本版本结算数量
     * @param taxedUnitPrice 含税单价
     * @param netAmount 未税货款
     * @param taxAmount 税额
     */
    public record SettlementLine(
            String purchaseOrderLineId,
            Quantity settlementQuantity,
            Money taxedUnitPrice,
            Money netAmount,
            Money taxAmount
    ) {

        /** 校验并创建一个正数量结算行。 */
        public SettlementLine {
            require(
                    purchaseOrderLineId != null && !purchaseOrderLineId.isBlank(),
                    "订单行 ID 不能为空"
            );
            require(
                    settlementQuantity != null && settlementQuantity.isPositive(),
                    "结算数量必须大于零"
            );
        }
    }

    /**
     * 由物流、质量或人工调整产生的费用事实。
     *
     * @param factId 费用事实唯一标识，用于幂等入账
     * @param feeType 费用业务类型
     * @param direction 加项或扣项方向
     * @param amount 非负费用金额
     * @param sourceReference 产生费用的外部单据或业务事实引用
     */
    public record FeeItem(
            String factId,
            FeeType feeType,
            FeeDirection direction,
            Money amount,
            String sourceReference
    ) {

        /** 校验并创建一条非负费用事实。 */
        public FeeItem {
            require(factId != null && !factId.isBlank(), "费用事实 ID 不能为空");
            require(feeType != null, "费用类型不能为空");
            require(direction != null, "费用方向不能为空");
            require(amount != null && amount.amount().signum() >= 0, "费用金额不能为负数");
            require(
                    sourceReference != null && !sourceReference.isBlank(),
                    "费用来源引用不能为空"
            );
        }
    }

    /** 采购结算支持的费用业务类型。 */
    public enum FeeType {
        FREIGHT,
        CONTAINER,
        TRANSIT,
        DDP,
        QUALITY_DEDUCTION,
        OTHER
    }

    /** 费用对应付金额的影响方向。 */
    public enum FeeDirection {
        ADD,
        DEDUCT
    }
}
