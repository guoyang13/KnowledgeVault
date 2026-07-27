package com.bo.rt.biz.scm.purchaseorder.next.interfaces.feign;

import com.bo.rt.biz.scm.purchaseorder.next.api.PurchaseOrderFacade;
import com.bo.rt.biz.scm.purchaseorder.next.api.dto.CreatePurchaseOrderRequest;
import com.bo.rt.biz.scm.purchaseorder.next.api.dto.PurchaseOrderResponse;
import com.bo.rt.biz.scm.purchaseorder.next.application.ordering.OrderingApplicationService;
import com.bo.rt.biz.scm.purchaseorder.next.application.ordering.OrderingApplicationService.CreateOrderFromRequisition;
import com.bo.rt.biz.scm.purchaseorder.next.application.ordering.OrderingApplicationService.LineCommercialTerms;
import com.bo.rt.biz.scm.purchaseorder.next.application.ordering.OrderingApplicationService.OrderResult;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.PurchaseOrder;
import com.bo.rt.biz.scm.purchaseorder.next.domain.ordering.model.ProcurementRoute;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Money;
import java.util.stream.Collectors;

/**
 * 采购订单 Feign 入口适配器。
 *
 * <p>负责兼容旧接口路径或暴露新接口契约，并把外部请求转换为应用层命令。</p>
 */
public class PurchaseOrderFeignFacade implements PurchaseOrderFacade {

    /** 采购订单写用例。 */
    private final OrderingApplicationService orderingService;

    /** 注入采购订单应用服务。 */
    public PurchaseOrderFeignFacade(OrderingApplicationService orderingService) {
        this.orderingService = orderingService;
    }

    /**
     * 创建采购订单草稿。
     *
     * @param request 创建采购订单的外部请求参数
     * @return 创建后的采购订单摘要
     */
    @Override
    public PurchaseOrderResponse createDraft(CreatePurchaseOrderRequest request) {
        var lineTerms = request.lines().stream().collect(Collectors.toMap(
                line -> line.requisitionLineId(),
                line -> new LineCommercialTerms(
                        line.legacyPsoCode(),
                        new Money(line.taxedUnitPrice(), request.currency()),
                        line.expectedArrivalDate()
                )
        ));
        OrderResult result = orderingService.createFromRequisition(
                new CreateOrderFromRequisition(
                        request.commandId(),
                        request.reservationId(),
                        request.buyerOrganizationId(),
                        request.destinationCode(),
                        ProcurementRoute.DeliveryMethod.valueOf(request.deliveryMethod()),
                        request.transitNodeCode(),
                        new PurchaseOrder.DeliveryTerms(
                                request.incoterm(),
                                request.supplierArrangesTransport(),
                                request.allowOverDelivery()
                        ),
                        lineTerms
                )
        );
        return toResponse(result);
    }

    /**
     * 提交采购订单。
     *
     * @param purchaseOrderCode 采购订单号
     * @return 提交后的采购订单摘要
     */
    @Override
    public PurchaseOrderResponse submit(String purchaseOrderCode) {
        OrderResult current = orderingService.getByOrderNo(purchaseOrderCode);
        return toResponse(orderingService.submit(purchaseOrderCode, current.writeVersion()));
    }

    /**
     * 查询采购订单摘要。
     *
     * @param purchaseOrderCode 采购订单号
     * @return 采购订单摘要
     */
    @Override
    public PurchaseOrderResponse getByCode(String purchaseOrderCode) {
        return toResponse(orderingService.getByOrderNo(purchaseOrderCode));
    }

    /** 将应用层结果转换为稳定的对外响应契约。 */
    private PurchaseOrderResponse toResponse(OrderResult result) {
        return new PurchaseOrderResponse(
                result.purchaseOrderId(),
                result.orderNo(),
                result.supplierCode(),
                result.status(),
                result.approvalStatus(),
                result.writeVersion()
        );
    }
}
