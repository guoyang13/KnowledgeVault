package com.bo.rt.biz.scm.purchaseorder.next.application.settlement;

import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.DomainEventPublisher;
import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.IdentityGenerator;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.event.OrderingEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.event.QualityEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.model.ProcurementSettlement;
import com.bo.rt.biz.scm.purchaseorder.next.domain.settlement.repository.ProcurementSettlementRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Money;
import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 采购结算用例，消费订单和质量事实并驱动结算计算。
 */
public class SettlementApplicationService {

    /** 采购结算聚合仓储。 */
    private final ProcurementSettlementRepository settlementRepository;
    /** 结算聚合标识生成器。 */
    private final IdentityGenerator identityGenerator;
    /** 随本地事务写入领域事件的发布端口。 */
    private final DomainEventPublisher eventPublisher;

    /** 注入采购结算用例依赖。 */
    public SettlementApplicationService(
            ProcurementSettlementRepository settlementRepository,
            IdentityGenerator identityGenerator,
            DomainEventPublisher eventPublisher
    ) {
        this.settlementRepository = settlementRepository;
        this.identityGenerator = identityGenerator;
        this.eventPublisher = eventPublisher;
    }

    /** 根据采购订单生效快照幂等初始化结算台账。 */
    public void onPurchaseOrderEffective(OrderingEvents.PurchaseOrderEffective event) {
        if (settlementRepository.findByPurchaseOrderId(event.aggregateId()).isPresent()) {
            return;
        }
        Map<String, Money> linePrices = event.lines().stream().collect(Collectors.toMap(
                OrderingEvents.OrderLineSnapshot::purchaseOrderLineId,
                OrderingEvents.OrderLineSnapshot::taxedUnitPrice
        ));
        ProcurementSettlement settlement = ProcurementSettlement.initialize(
                identityGenerator.nextId("procurement-settlement"),
                event.aggregateId(),
                event.supplierCode(),
                event.buyerOrganizationId(),
                event.currency(),
                event.aggregateVersion(),
                linePrices
        );
        settlementRepository.save(settlement);
        eventPublisher.publishAll(settlement.pullDomainEvents());
    }

    /** 将质量上下文确认的合格数量按事件和订单行幂等计入结算。 */
    public void onGoodsAccepted(QualityEvents.GoodsAcceptedForSettlement event) {
        ProcurementSettlement settlement = requireByOrderId(event.purchaseOrderId());
        event.lines().forEach(line -> settlement.applyAcceptedQuantity(
                event.eventId() + ":" + line.purchaseOrderLineId(),
                line.purchaseOrderLineId(),
                line.acceptedQuantity()
        ));
        settlementRepository.save(settlement);
        eventPublisher.publishAll(settlement.pullDomainEvents());
    }

    /** 将更新后的采购订单行价格和订单版本应用到结算依据。 */
    public void onPriceAdjusted(OrderingEvents.PurchaseOrderPriceAdjusted event) {
        ProcurementSettlement settlement = requireByOrderId(event.aggregateId());
        settlement.applyPriceAdjustment(
                event.lineId(), event.taxedUnitPrice(), event.aggregateVersion()
        );
        settlementRepository.save(settlement);
        eventPublisher.publishAll(settlement.pullDomainEvents());
    }

    /** 使用指定规则版本计算并确认一个新的结算版本。 */
    public void recalculateAndConfirm(
            String purchaseOrderId,
            BigDecimal taxRate,
            String taxRuleVersion,
            String feeRuleVersion
    ) {
        ProcurementSettlement settlement = requireByOrderId(purchaseOrderId);
        settlement.recalculate(taxRate, taxRuleVersion, feeRuleVersion);
        settlement.confirm();
        settlementRepository.save(settlement);
        eventPublisher.publishAll(settlement.pullDomainEvents());
    }

    /** 按采购订单加载结算聚合，不存在时终止用例。 */
    private ProcurementSettlement requireByOrderId(String purchaseOrderId) {
        return settlementRepository.findByPurchaseOrderId(purchaseOrderId)
                .orElseThrow(() -> new IllegalArgumentException("采购结算不存在"));
    }
}
