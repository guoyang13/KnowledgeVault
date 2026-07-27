package com.bo.rt.biz.scm.purchaseorder.next.application.qualityinspection;

import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.DomainEventPublisher;
import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.IdentityGenerator;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model.QualityInspectionOrder;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model.QualityInspectionOrder.Defect;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model.QualityInspectionOrder.InspectionDecision;
import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.repository.QualityInspectionOrderRepository;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.BusinessReference;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import java.util.List;

/**
 * 质量检验用例，负责把收货事实转换为质检单并协调质检处理。
 */
public class QualityInspectionApplicationService {

    /** 质量检验单聚合仓储。 */
    private final QualityInspectionOrderRepository inspectionRepository;
    /** 质检单标识生成器。 */
    private final IdentityGenerator identityGenerator;
    /** 随本地事务写入领域事件的发布端口。 */
    private final DomainEventPublisher eventPublisher;

    /** 注入质量检验用例依赖。 */
    public QualityInspectionApplicationService(
            QualityInspectionOrderRepository inspectionRepository,
            IdentityGenerator identityGenerator,
            DomainEventPublisher eventPublisher
    ) {
        this.inspectionRepository = inspectionRepository;
        this.identityGenerator = identityGenerator;
        this.eventPublisher = eventPublisher;
    }

    /**
     * 根据收货批次幂等创建质检单。
     *
     * @return 已存在或新建的质检单标识
     */
    public String createInspectionFromReceipt(ReceiptAccepted receipt) {
        return inspectionRepository.findByFulfillmentUnitId(receipt.fulfillmentUnitId())
                .map(QualityInspectionOrder::id)
                .orElseGet(() -> createInspection(receipt));
    }

    /** 创建并保存收货批次对应的质检单。 */
    private String createInspection(ReceiptAccepted receipt) {
        QualityInspectionOrder inspection = QualityInspectionOrder.createFromReceipt(
                identityGenerator.nextId("quality-inspection"),
                receipt.inspectionNo(),
                receipt.purchaseOrderId(),
                receipt.fulfillmentUnitId(),
                new BusinessReference(receipt.sourceType(), receipt.sourceBusinessNo()),
                receipt.mode(),
                receipt.lines().stream()
                        .map(line -> new QualityInspectionOrder.InspectionLine(
                                line.purchaseOrderLineId(),
                                line.receivedQuantity(),
                                line.inspectionRequired()
                        ))
                        .toList()
        );
        inspectionRepository.save(inspection);
        eventPublisher.publishAll(inspection.pullDomainEvents());
        return inspection.id();
    }

    /** 启动质检并固定本次采用的检验标准版本。 */
    public void start(String inspectionId, String standardVersion) {
        QualityInspectionOrder inspection = requireInspection(inspectionId);
        inspection.start(standardVersion);
        inspectionRepository.save(inspection);
        eventPublisher.publishAll(inspection.pullDomainEvents());
    }

    /** 记录一个采购订单行的数量判定、缺陷及例外授权。 */
    public void recordLineResult(RecordInspectionLineResult command) {
        QualityInspectionOrder inspection = requireInspection(command.inspectionId());
        inspection.recordLineResult(
                command.purchaseOrderLineId(),
                command.inspected(),
                command.accepted(),
                command.rejected(),
                command.decision(),
                command.defects(),
                command.authorizationReference()
        );
        inspectionRepository.save(inspection);
        eventPublisher.publishAll(inspection.pullDomainEvents());
    }

    /** 在所有必检行完成判定后结束整张质检单。 */
    public void finish(String inspectionId) {
        QualityInspectionOrder inspection = requireInspection(inspectionId);
        inspection.finish();
        inspectionRepository.save(inspection);
        eventPublisher.publishAll(inspection.pullDomainEvents());
    }

    /** 加载质检单，不存在时终止用例。 */
    private QualityInspectionOrder requireInspection(String inspectionId) {
        return inspectionRepository.findById(inspectionId)
                .orElseThrow(() -> new IllegalArgumentException("质检单不存在"));
    }

    /**
     * 已确认收货、需要进入质量处理的输入事实。
     *
     * @param inspectionNo 质检业务单号
     * @param purchaseOrderId 采购订单标识
     * @param fulfillmentUnitId 收货对应的履约单元标识
     * @param sourceType 来源单据类型
     * @param sourceBusinessNo 来源单据号
     * @param mode 质检模式
     * @param lines 收货行快照
     */
    public record ReceiptAccepted(
            String inspectionNo,
            String purchaseOrderId,
            String fulfillmentUnitId,
            String sourceType,
            String sourceBusinessNo,
            QualityInspectionOrder.InspectionMode mode,
            List<ReceiptLine> lines
    ) {

        /** 固化收货行列表。 */
        public ReceiptAccepted {
            lines = List.copyOf(lines);
        }
    }

    /**
     * 一条收货数量及其质检要求。
     *
     * @param purchaseOrderLineId 采购订单行标识
     * @param receivedQuantity 本批次实收数量
     * @param inspectionRequired 是否必须质检
     */
    public record ReceiptLine(
            String purchaseOrderLineId,
            Quantity receivedQuantity,
            boolean inspectionRequired
    ) {
    }

    /**
     * 记录一个质检行结果的命令。
     *
     * @param inspectionId 质检单标识
     * @param purchaseOrderLineId 采购订单行标识
     * @param inspected 已检数量
     * @param accepted 合格数量
     * @param rejected 不合格数量
     * @param decision 行判定结论
     * @param defects 缺陷记录
     * @param authorizationReference 例外放行时的授权依据
     */
    public record RecordInspectionLineResult(
            String inspectionId,
            String purchaseOrderLineId,
            Quantity inspected,
            Quantity accepted,
            Quantity rejected,
            InspectionDecision decision,
            List<Defect> defects,
            String authorizationReference
    ) {

        /** 将空缺陷列表规范化为空集合并固化。 */
        public RecordInspectionLineResult {
            defects = defects == null ? List.of() : List.copyOf(defects);
        }
    }
}
