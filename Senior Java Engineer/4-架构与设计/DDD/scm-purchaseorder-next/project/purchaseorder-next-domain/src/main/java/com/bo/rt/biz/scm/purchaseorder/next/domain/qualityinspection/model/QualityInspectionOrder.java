package com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.event.QualityEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.AbstractAggregateRoot;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.BusinessReference;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.Quantity;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 到货质检聚合，按 fulfillmentUnitId 隔离每个实际收货批次。
 */
public class QualityInspectionOrder extends AbstractAggregateRoot {

    /** 质检单聚合标识。 */
    private final String id;

    /** 质量人员使用的质检业务单号。 */
    private final String inspectionNo;

    /** 被检货物所属采购订单。 */
    private final String purchaseOrderId;

    /** 被检实际收货或发运批次的统一关联标识。 */
    private final String fulfillmentUnitId;

    /** 仓储收货单等权威来源业务引用。 */
    private final BusinessReference source;

    /** 本次检验采用的全检、抽检或单证检验模式。 */
    private final InspectionMode mode;

    /** 按采购订单行记录的收货与检验结论。 */
    private final List<InspectionLine> lines;

    /** 质检单整体生命周期状态。 */
    private InspectionStatus status;

    /** 开始检验时冻结的质量标准版本。 */
    private String standardVersion;

    /** 根据权威收货来源创建待检聚合并发布质检要求。 */
    private QualityInspectionOrder(
            String id,
            String inspectionNo,
            String purchaseOrderId,
            String fulfillmentUnitId,
            BusinessReference source,
            InspectionMode mode,
            List<InspectionLine> lines
    ) {
        require(id != null && !id.isBlank(), "质检单 ID 不能为空");
        require(inspectionNo != null && !inspectionNo.isBlank(), "质检单号不能为空");
        require(purchaseOrderId != null && !purchaseOrderId.isBlank(), "采购订单 ID 不能为空");
        require(fulfillmentUnitId != null && !fulfillmentUnitId.isBlank(), "履约单元 ID 不能为空");
        require(source != null, "收货来源不能为空");
        require(mode != null, "质检模式不能为空");
        require(lines != null && !lines.isEmpty(), "质检单至少包含一行");
        this.id = id;
        this.inspectionNo = inspectionNo;
        this.purchaseOrderId = purchaseOrderId;
        this.fulfillmentUnitId = fulfillmentUnitId;
        this.source = source;
        this.mode = mode;
        this.lines = new ArrayList<>(lines);
        this.status = InspectionStatus.PENDING;
        long eventVersion = nextVersion();
        raise(new QualityEvents.QualityInspectionRequired(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                inspectionNo,
                purchaseOrderId,
                fulfillmentUnitId
        ));
    }

    /**
     * 根据一个明确的收货批次创建待检质检单。
     */
    public static QualityInspectionOrder createFromReceipt(
            String id,
            String inspectionNo,
            String purchaseOrderId,
            String fulfillmentUnitId,
            BusinessReference source,
            InspectionMode mode,
            List<InspectionLine> lines
    ) {
        return new QualityInspectionOrder(
                id, inspectionNo, purchaseOrderId, fulfillmentUnitId, source, mode, lines
        );
    }

    /**
     * 开始或重开质检，并冻结本轮适用的质量标准版本。
     */
    public void start(String standardVersion) {
        require(
                status == InspectionStatus.PENDING || status == InspectionStatus.REOPENED,
                "当前质检单不能开始"
        );
        require(standardVersion != null && !standardVersion.isBlank(), "质检标准版本不能为空");
        this.standardVersion = standardVersion;
        status = InspectionStatus.IN_PROGRESS;
        long eventVersion = nextVersion();
        raise(new QualityEvents.QualityInspectionStarted(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                inspectionNo,
                standardVersion
        ));
    }

    /**
     * 记录订单行检验数量、接收量、拒收量、决定和缺陷证据。
     */
    public void recordLineResult(
            String purchaseOrderLineId,
            Quantity inspected,
            Quantity accepted,
            Quantity rejected,
            InspectionDecision decision,
            List<Defect> defects,
            String authorizationReference
    ) {
        require(status == InspectionStatus.IN_PROGRESS, "质检单不在执行中");
        InspectionLine line = requireLine(purchaseOrderLineId);
        line.recordResult(
                inspected, accepted, rejected, decision, defects, authorizationReference
        );
        long eventVersion = nextVersion();
        if (accepted.isPositive()) {
            raise(new QualityEvents.InspectionLineAccepted(
                    UUID.randomUUID().toString(),
                    id,
                    eventVersion,
                    Instant.now(),
                    inspectionNo,
                    purchaseOrderLineId,
                    accepted
            ));
        }
        if (rejected.isPositive()) {
            long rejectedVersion = nextVersion();
            raise(new QualityEvents.InspectionLineRejected(
                    UUID.randomUUID().toString(),
                    id,
                    rejectedVersion,
                    Instant.now(),
                    inspectionNo,
                    purchaseOrderLineId,
                    rejected
            ));
        }
        if (lines.stream().allMatch(InspectionLine::hasDecision)) {
            status = InspectionStatus.PENDING_DECISION;
        }
    }

    /**
     * 因等待资料、复核等原因暂时挂起执行中的质检。
     */
    public void suspend(String reason) {
        require(status == InspectionStatus.IN_PROGRESS, "只有执行中的质检单可以挂起");
        require(reason != null && !reason.isBlank(), "挂起原因不能为空");
        status = InspectionStatus.SUSPENDED;
        nextVersion();
    }

    /** 恢复被挂起的质检执行。 */
    public void resume() {
        require(status == InspectionStatus.SUSPENDED, "质检单未挂起");
        status = InspectionStatus.IN_PROGRESS;
        nextVersion();
    }

    /**
     * 完成全部必检行，并分别发布可结算接收量和质量拒收量。
     */
    public void finish() {
        require(status == InspectionStatus.PENDING_DECISION, "仍有质检行未提交结论");
        require(lines.stream().allMatch(InspectionLine::hasDecision), "仍有必检行未完成");
        status = InspectionStatus.COMPLETED;
        long eventVersion = nextVersion();
        raise(new QualityEvents.QualityInspectionCompleted(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                inspectionNo,
                purchaseOrderId,
                fulfillmentUnitId
        ));

        List<QualityEvents.AcceptedLine> acceptedLines = lines.stream()
                .filter(line -> line.accepted().isPositive())
                .map(line -> new QualityEvents.AcceptedLine(
                        line.purchaseOrderLineId(), line.accepted()
                ))
                .toList();
        if (!acceptedLines.isEmpty()) {
            long acceptedVersion = nextVersion();
            raise(new QualityEvents.GoodsAcceptedForSettlement(
                    UUID.randomUUID().toString(),
                    id,
                    acceptedVersion,
                    Instant.now(),
                    inspectionNo,
                    purchaseOrderId,
                    fulfillmentUnitId,
                    acceptedLines
            ));
        }

        List<QualityEvents.RejectedLine> rejectedLines = lines.stream()
                .filter(line -> line.rejected().isPositive())
                .map(line -> new QualityEvents.RejectedLine(
                        line.purchaseOrderLineId(), line.rejected(), line.decision().name()
                ))
                .toList();
        if (!rejectedLines.isEmpty()) {
            long rejectedVersion = nextVersion();
            raise(new QualityEvents.GoodsRejectedByQuality(
                    UUID.randomUUID().toString(),
                    id,
                    rejectedVersion,
                    Instant.now(),
                    inspectionNo,
                    purchaseOrderId,
                    fulfillmentUnitId,
                    rejectedLines
            ));
        }
    }

    /**
     * 经授权重开已完成质检，历史结论进入行级版本记录。
     */
    public void reopen(String authorizationReference, String reason) {
        require(status == InspectionStatus.COMPLETED, "只有已完成质检单可以重开");
        require(
                authorizationReference != null && !authorizationReference.isBlank(),
                "重开授权依据不能为空"
        );
        require(reason != null && !reason.isBlank(), "重开原因不能为空");
        lines.forEach(InspectionLine::prepareForReinspection);
        status = InspectionStatus.REOPENED;
        long eventVersion = nextVersion();
        raise(new QualityEvents.QualityInspectionReopened(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                inspectionNo,
                authorizationReference,
                reason
        ));
    }

    /** 查找目标质检行，不存在时拒绝命令。 */
    private InspectionLine requireLine(String purchaseOrderLineId) {
        return lines.stream()
                .filter(line -> line.purchaseOrderLineId().equals(purchaseOrderLineId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("质检行不存在: " + purchaseOrderLineId));
    }

    /** 返回质检单聚合标识。 */
    public String id() {
        return id;
    }

    /** 返回质检业务单号。 */
    public String inspectionNo() {
        return inspectionNo;
    }

    /** 返回对应采购订单标识。 */
    public String purchaseOrderId() {
        return purchaseOrderId;
    }

    /** 返回实际履约批次标识。 */
    public String fulfillmentUnitId() {
        return fulfillmentUnitId;
    }

    /** 返回质检单整体状态。 */
    public InspectionStatus status() {
        return status;
    }

    /** 返回质检行只读副本。 */
    public List<InspectionLine> lines() {
        return List.copyOf(lines);
    }

    public enum InspectionMode {
        FULL,
        SAMPLING,
        DOCUMENT_ONLY
    }

    public enum InspectionStatus {
        PENDING,
        IN_PROGRESS,
        PENDING_DECISION,
        SUSPENDED,
        COMPLETED,
        REOPENED
    }

    public enum InspectionDecision {
        ACCEPTED,
        REJECTED,
        PARTIALLY_ACCEPTED,
        CONCESSION_ACCEPTED,
        REWORK_REQUIRED
    }

    public enum DefectSeverity {
        MINOR,
        MAJOR,
        CRITICAL
    }

    /** 单个采购订单行在本批收货中的质检数量账和决定。 */
    public static final class InspectionLine {

        /** 被检采购订单行标识。 */
        private final String purchaseOrderLineId;

        /** 本履约批次的权威收货数量。 */
        private final Quantity received;

        /** 本行是否属于必须给出结论的检验项。 */
        private final boolean required;

        /** 重开前保存的不可变历史决定版本。 */
        private final List<LineDecisionVersion> history = new ArrayList<>();

        /** 本轮已经检查的数量。 */
        private Quantity inspected;

        /** 本轮允许进入后续入库或结算的数量。 */
        private Quantity accepted;

        /** 本轮因质量问题拒收的数量。 */
        private Quantity rejected;

        /** 本轮行级质量决定。 */
        private InspectionDecision decision;

        /** 支撑拒收或返工决定的缺陷集合。 */
        private List<Defect> defects = List.of();

        /** 让步接收等特殊决定的授权依据。 */
        private String authorizationReference;

        /**
         * 根据收货行创建尚未给出结论的质检行。
         */
        public InspectionLine(String purchaseOrderLineId, Quantity received, boolean required) {
            require(
                    purchaseOrderLineId != null && !purchaseOrderLineId.isBlank(),
                    "订单行 ID 不能为空"
            );
            require(received != null && received.isPositive(), "收货数量必须大于零");
            this.purchaseOrderLineId = purchaseOrderLineId;
            this.received = received;
            this.required = required;
            this.inspected = Quantity.zero(received.unit());
            this.accepted = Quantity.zero(received.unit());
            this.rejected = Quantity.zero(received.unit());
        }

        /**
         * 写入本轮质检结论并校验数量、缺陷证据和特殊授权。
         */
        private void recordResult(
                Quantity inspected,
                Quantity accepted,
                Quantity rejected,
                InspectionDecision decision,
                List<Defect> defects,
                String authorizationReference
        ) {
            require(inspected != null && inspected.lessThanOrEqualTo(received), "检验数量超过收货数量");
            require(accepted != null && rejected != null, "接收和拒收数量不能为空");
            require(accepted.add(rejected).lessThanOrEqualTo(inspected), "接收与拒收数量超过检验数量");
            require(decision != null, "质检决定不能为空");
            require(
                    !rejected.isPositive() || (defects != null && !defects.isEmpty()),
                    "存在拒收数量时必须记录缺陷与证据"
            );
            require(
                    decision != InspectionDecision.CONCESSION_ACCEPTED
                            || (authorizationReference != null && !authorizationReference.isBlank()),
                    "让步接收必须记录授权依据"
            );
            this.inspected = inspected;
            this.accepted = accepted;
            this.rejected = rejected;
            this.decision = decision;
            this.defects = defects == null ? List.of() : List.copyOf(defects);
            this.authorizationReference = authorizationReference;
        }

        /** 判断本行是否满足质检单完成条件。 */
        private boolean hasDecision() {
            return !required || decision != null;
        }

        /** 保存当前决定版本并重置本轮可变结论。 */
        private void prepareForReinspection() {
            if (decision != null) {
                history.add(new LineDecisionVersion(
                        history.size() + 1,
                        inspected,
                        accepted,
                        rejected,
                        decision,
                        defects,
                        authorizationReference
                ));
            }
            inspected = Quantity.zero(received.unit());
            accepted = Quantity.zero(received.unit());
            rejected = Quantity.zero(received.unit());
            decision = null;
            defects = List.of();
            authorizationReference = null;
        }

        /** 返回采购订单行标识。 */
        public String purchaseOrderLineId() {
            return purchaseOrderLineId;
        }

        /** 返回权威收货数量。 */
        public Quantity received() {
            return received;
        }

        /** 返回本轮已检数量。 */
        public Quantity inspected() {
            return inspected;
        }

        /** 返回本轮接收数量。 */
        public Quantity accepted() {
            return accepted;
        }

        /** 返回本轮拒收数量。 */
        public Quantity rejected() {
            return rejected;
        }

        /** 返回本轮质量决定。 */
        public InspectionDecision decision() {
            return decision;
        }

        /** 返回重开前的历史决定版本。 */
        public List<LineDecisionVersion> history() {
            return List.copyOf(history);
        }
    }

    /**
     * 质检缺陷。
     *
     * @param defectCode 标准缺陷编码
     * @param severity 缺陷严重程度
     * @param quantity 受影响数量
     * @param evidence 图片、报告等证据引用
     */
    public record Defect(
            String defectCode,
            DefectSeverity severity,
            Quantity quantity,
            List<SampleMatchingCase.Evidence> evidence
    ) {

        /** 校验并创建一条可追溯的缺陷记录。 */
        public Defect {
            require(defectCode != null && !defectCode.isBlank(), "缺陷编码不能为空");
            require(severity != null, "缺陷级别不能为空");
            require(quantity != null && quantity.isPositive(), "缺陷数量必须大于零");
            require(evidence != null && !evidence.isEmpty(), "缺陷证据不能为空");
            evidence = List.copyOf(evidence);
        }
    }

    /**
     * 质检重开前保存的行级决定版本。
     *
     * @param version 行决定版本号
     * @param inspected 已检数量
     * @param accepted 接收数量
     * @param rejected 拒收数量
     * @param decision 质量决定
     * @param defects 缺陷集合
     * @param authorizationReference 特殊决定授权依据
     */
    public record LineDecisionVersion(
            int version,
            Quantity inspected,
            Quantity accepted,
            Quantity rejected,
            InspectionDecision decision,
            List<Defect> defects,
            String authorizationReference
    ) {

        /** 固化历史决定中的缺陷列表。 */
        public LineDecisionVersion {
            defects = List.copyOf(defects);
        }
    }
}
