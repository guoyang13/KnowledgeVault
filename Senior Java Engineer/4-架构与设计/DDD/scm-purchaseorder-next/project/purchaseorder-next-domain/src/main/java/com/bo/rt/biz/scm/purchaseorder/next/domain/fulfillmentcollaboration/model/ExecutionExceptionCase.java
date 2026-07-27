package com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.fulfillmentcollaboration.event.CollaborationEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.AbstractAggregateRoot;
import java.time.Instant;
import java.util.UUID;

/**
 * 履约异常聚合。businessKey 的唯一性由仓储约束，防止重复开未关闭异常。
 */
public class ExecutionExceptionCase extends AbstractAggregateRoot {

    /** 异常处理单标识。 */
    private final String id;
    /** 异常所属的采购订单标识。 */
    private final String purchaseOrderId;
    /** 标识同一异常来源的业务键，由仓储用于防止重复开单。 */
    private final String businessKey;
    /** 异常业务类型。 */
    private final ExceptionType type;
    /** 异常由任务触发时对应的执行任务标识，可以为空。 */
    private final String relatedTaskId;
    /** 当前异常严重程度。 */
    private Severity severity;
    /** 异常处理状态。 */
    private ExceptionStatus status;
    /** 当前异常负责人。 */
    private String owner;
    /** 最终解决方案或处置结论。 */
    private String resolution;

    /** 创建处于待处理状态的履约异常。 */
    private ExecutionExceptionCase(
            String id,
            String purchaseOrderId,
            String businessKey,
            ExceptionType type,
            Severity severity,
            String relatedTaskId
    ) {
        require(id != null && !id.isBlank(), "异常 ID 不能为空");
        require(purchaseOrderId != null && !purchaseOrderId.isBlank(), "采购订单 ID 不能为空");
        require(businessKey != null && !businessKey.isBlank(), "异常业务键不能为空");
        require(type != null, "异常类型不能为空");
        require(severity != null, "异常级别不能为空");
        this.id = id;
        this.purchaseOrderId = purchaseOrderId;
        this.businessKey = businessKey;
        this.type = type;
        this.severity = severity;
        this.relatedTaskId = relatedTaskId;
        this.status = ExceptionStatus.OPEN;
        long eventVersion = nextVersion();
        raise(new CollaborationEvents.ExecutionExceptionOpened(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                businessKey,
                type.name(),
                severity.name(),
                relatedTaskId
        ));
    }

    /** 根据稳定业务键打开一个履约异常处理单。 */
    public static ExecutionExceptionCase open(
            String id,
            String purchaseOrderId,
            String businessKey,
            ExceptionType type,
            Severity severity,
            String relatedTaskId
    ) {
        return new ExecutionExceptionCase(
                id, purchaseOrderId, businessKey, type, severity, relatedTaskId
        );
    }

    /** 为待处理或已升级异常指定负责人。 */
    public void assign(String owner) {
        require(status == ExceptionStatus.OPEN || status == ExceptionStatus.ESCALATED, "当前异常不能分配");
        require(owner != null && !owner.isBlank(), "异常负责人不能为空");
        this.owner = owner;
        status = ExceptionStatus.ASSIGNED;
        nextVersion();
    }

    /** 将已分配异常推进到处理中。 */
    public void startProcessing() {
        require(
                status == ExceptionStatus.ASSIGNED || status == ExceptionStatus.ESCALATED,
                "当前异常不能开始处理"
        );
        status = ExceptionStatus.PROCESSING;
        nextVersion();
    }

    /** 提升异常严重级别并记录升级原因。 */
    public void escalate(Severity newSeverity, String reason) {
        require(
                status == ExceptionStatus.ASSIGNED || status == ExceptionStatus.PROCESSING,
                "当前异常不能升级"
        );
        require(newSeverity != null && newSeverity.ordinal() > severity.ordinal(), "新级别必须更高");
        require(reason != null && !reason.isBlank(), "升级原因不能为空");
        severity = newSeverity;
        status = ExceptionStatus.ESCALATED;
        long eventVersion = nextVersion();
        raise(new CollaborationEvents.ExecutionExceptionEscalated(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                businessKey,
                severity.name(),
                reason
        ));
    }

    /** 记录解决方案并将异常标记为已解决。 */
    public void resolve(String resolution) {
        require(
                status == ExceptionStatus.PROCESSING || status == ExceptionStatus.ESCALATED,
                "当前异常不能解决"
        );
        require(resolution != null && !resolution.isBlank(), "解决方案不能为空");
        this.resolution = resolution;
        status = ExceptionStatus.RESOLVED;
        long eventVersion = nextVersion();
        raise(new CollaborationEvents.ExecutionExceptionResolved(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                purchaseOrderId,
                businessKey,
                resolution
        ));
    }

    /** 在解决方案确认后关闭异常。 */
    public void close() {
        require(status == ExceptionStatus.RESOLVED, "只有已解决异常可以关闭");
        status = ExceptionStatus.CLOSED;
        nextVersion();
    }

    /** 返回异常处理单标识。 */
    public String id() {
        return id;
    }

    /** 返回异常来源业务键。 */
    public String businessKey() {
        return businessKey;
    }

    /** 返回异常处理状态。 */
    public ExceptionStatus status() {
        return status;
    }

    /** 返回当前严重程度。 */
    public Severity severity() {
        return severity;
    }

    /** 能够触发协同处置的履约异常类型。 */
    public enum ExceptionType {
        TASK_OVERDUE,
        SUPPLIER_REJECTED,
        SHIPMENT_BLOCKED,
        QUALITY_REJECTED,
        QUANTITY_DISCREPANCY,
        SETTLEMENT_REJECTED
    }

    /** 异常影响程度，用于升级规则和处理优先级。 */
    public enum Severity {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    /** 履约异常从打开到关闭的处理状态。 */
    public enum ExceptionStatus {
        OPEN,
        ASSIGNED,
        PROCESSING,
        ESCALATED,
        RESOLVED,
        CLOSED
    }
}
