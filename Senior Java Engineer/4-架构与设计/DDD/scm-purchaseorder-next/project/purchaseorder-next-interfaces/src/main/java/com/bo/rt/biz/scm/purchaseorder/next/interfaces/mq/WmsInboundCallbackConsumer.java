package com.bo.rt.biz.scm.purchaseorder.next.interfaces.mq;

import com.bo.rt.biz.scm.purchaseorder.next.application.qualityinspection.QualityInspectionApplicationService;
import com.bo.rt.biz.scm.purchaseorder.next.application.qualityinspection.QualityInspectionApplicationService.ReceiptAccepted;
import com.bo.rt.biz.scm.purchaseorder.next.application.qualityinspection.QualityInspectionApplicationService.ReceiptLine;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model.QualityInspectionOrder;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import java.util.List;

/**
 * WMS 入库回调消息消费者。
 *
 * <p>消息入口只负责幂等、协议转换和调用应用服务，不直接修改领域对象或数据库。</p>
 */
public class WmsInboundCallbackConsumer {

    /** 质量检验应用服务。 */
    private final QualityInspectionApplicationService qualityService;

    /** 注入质量检验用例。 */
    public WmsInboundCallbackConsumer(QualityInspectionApplicationService qualityService) {
        this.qualityService = qualityService;
    }

    /**
     * 处理 WMS 入库回调消息。
     *
     * @param message 已由消息协议适配器完成反序列化的 WMS 消息
     */
    public void onMessage(WmsReceiptMessage message) {
        qualityService.createInspectionFromReceipt(new ReceiptAccepted(
                message.inspectionNo(),
                message.purchaseOrderId(),
                message.fulfillmentUnitId(),
                "WAREHOUSE_RECEIPT",
                message.receiptNo(),
                QualityInspectionOrder.InspectionMode.valueOf(message.inspectionMode()),
                message.lines().stream()
                        .map(line -> new ReceiptLine(
                                line.purchaseOrderLineId(),
                                new Quantity(line.receivedQuantity(), line.unit()),
                                line.inspectionRequired()
                        ))
                        .toList()
        ));
    }

    /**
     * WMS 收货回调消息。
     *
     * @param messageId 消息幂等标识
     * @param receiptNo WMS 收货单号
     * @param inspectionNo 待创建的质检单号
     * @param purchaseOrderId 采购订单标识
     * @param fulfillmentUnitId 收货对应的履约单元标识
     * @param inspectionMode 质检模式编码
     * @param lines 收货行列表
     */
    public record WmsReceiptMessage(
            String messageId,
            String receiptNo,
            String inspectionNo,
            String purchaseOrderId,
            String fulfillmentUnitId,
            String inspectionMode,
            List<WmsReceiptLine> lines
    ) {

        /** 固化消息中的收货行列表。 */
        public WmsReceiptMessage {
            lines = List.copyOf(lines);
        }
    }

    /**
     * WMS 收货消息中的行项目。
     *
     * @param purchaseOrderLineId 采购订单行标识
     * @param receivedQuantity 实收数量数值
     * @param unit 数量单位
     * @param inspectionRequired 是否要求质检
     */
    public record WmsReceiptLine(
            String purchaseOrderLineId,
            java.math.BigDecimal receivedQuantity,
            String unit,
            boolean inspectionRequired
    ) {
    }
}
