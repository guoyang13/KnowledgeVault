package com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Money;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.SkuRef;
import java.time.LocalDate;

/**
 * 采购订单行实体。legacyPsoCode 仅用于迁移期兼容，不再作为子订单聚合。
 */
public class PurchaseOrderLine {

    /** 新模型中的采购订单行标识。 */
    private final String id;

    /** 迁移期用于兼容旧 PSO 的业务编码，不承担聚合职责。 */
    private final String legacyPsoCode;

    /** 消耗数量所属的采购申请行。 */
    private final String requisitionLineId;

    /** 下单时冻结的 SKU 引用。 */
    private final SkuRef sku;

    /** 已形成商业承诺的采购数量。 */
    private final Quantity orderedQuantity;

    /** 当前生效的含税采购单价。 */
    private Money taxedUnitPrice;

    /** 当前生效的预计到货日期。 */
    private LocalDate expectedArrivalDate;

    /**
     * 创建采购订单行并校验数量、单价和交期。
     */
    public PurchaseOrderLine(
            String id,
            String legacyPsoCode,
            String requisitionLineId,
            SkuRef sku,
            Quantity orderedQuantity,
            Money taxedUnitPrice,
            LocalDate expectedArrivalDate
    ) {
        require(id != null && !id.isBlank(), "订单行 ID 不能为空");
        require(sku != null, "SKU 不能为空");
        require(orderedQuantity != null && orderedQuantity.isPositive(), "订单数量必须大于零");
        require(taxedUnitPrice != null && taxedUnitPrice.amount().signum() >= 0, "采购单价不能为负数");
        require(expectedArrivalDate != null, "预计到货日期不能为空");
        this.id = id;
        this.legacyPsoCode = legacyPsoCode;
        this.requisitionLineId = requisitionLineId;
        this.sku = sku;
        this.orderedQuantity = orderedQuantity;
        this.taxedUnitPrice = taxedUnitPrice;
        this.expectedArrivalDate = expectedArrivalDate;
    }

    /** 应用已审批交期变更；调用方必须先校验变更单。 */
    void changeExpectedArrivalDate(LocalDate newDate) {
        require(newDate != null, "新交期不能为空");
        expectedArrivalDate = newDate;
    }

    /** 应用已审批调价，禁止借调价切换结算币种。 */
    void adjustPrice(Money newPrice) {
        require(newPrice != null && newPrice.amount().signum() >= 0, "新单价不能为负数");
        require(taxedUnitPrice.currency().equals(newPrice.currency()), "调价不能变更币种");
        taxedUnitPrice = newPrice;
    }

    /** 按当前含税单价计算订单行总额。 */
    public Money total() {
        return taxedUnitPrice.multiply(orderedQuantity);
    }

    /** 返回订单行标识。 */
    public String id() {
        return id;
    }

    /** 返回迁移期旧 PSO 编码。 */
    public String legacyPsoCode() {
        return legacyPsoCode;
    }

    /** 返回来源采购申请行标识。 */
    public String requisitionLineId() {
        return requisitionLineId;
    }

    /** 返回商品引用快照。 */
    public SkuRef sku() {
        return sku;
    }

    /** 返回已下单数量。 */
    public Quantity orderedQuantity() {
        return orderedQuantity;
    }

    /** 返回当前含税单价。 */
    public Money taxedUnitPrice() {
        return taxedUnitPrice;
    }

    /** 返回当前预计到货日期。 */
    public LocalDate expectedArrivalDate() {
        return expectedArrivalDate;
    }
}
