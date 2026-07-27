package com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.model;

import static com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.DomainRuleViolation.require;

import com.bo.rt.biz.scm.purchaseorder.next.domain.qualityinspection.event.QualityEvents;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.AbstractAggregateRoot;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.BusinessReference;
import com.bo.rt.biz.scm.purchaseorder.next.domain.shared.model.SkuRef;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 对样聚合。对样针对基准样品，生命周期独立于到货质检。
 */
public class SampleMatchingCase extends AbstractAggregateRoot {

    /** 对样聚合标识。 */
    private final String id;

    /** 触发对样要求的订单、订单行或发运业务引用。 */
    private final BusinessReference reference;

    /** 对样结论适用的业务范围。 */
    private final ReferenceScope scope;

    /** 需要逐项与基准样品比较的商品集合。 */
    private final List<SampleItem> items;

    /** 对样单整体生命周期状态。 */
    private SampleStatus status;

    /** 校验对样范围与对样项，并创建尚未开始的对样聚合。 */
    private SampleMatchingCase(
            String id,
            BusinessReference reference,
            ReferenceScope scope,
            List<SampleItem> items
    ) {
        require(id != null && !id.isBlank(), "对样单 ID 不能为空");
        require(reference != null, "对样来源不能为空");
        require(scope != null, "对样范围不能为空");
        require(items != null && !items.isEmpty(), "对样单至少包含一个样品项");
        this.id = id;
        this.reference = reference;
        this.scope = scope;
        this.items = new ArrayList<>(items);
        this.status = SampleStatus.NOT_STARTED;
        long eventVersion = nextVersion();
        raise(new QualityEvents.SampleMatchingRequired(
                UUID.randomUUID().toString(),
                id,
                eventVersion,
                Instant.now(),
                reference.type(),
                reference.businessNo()
        ));
    }

    /**
     * 创建尚未开始的对样单并发布对样要求事件。
     */
    public static SampleMatchingCase create(
            String id,
            BusinessReference reference,
            ReferenceScope scope,
            List<SampleItem> items
    ) {
        return new SampleMatchingCase(id, reference, scope, items);
    }

    /** 开始执行对样。 */
    public void start() {
        require(status == SampleStatus.NOT_STARTED, "对样单已经开始");
        status = SampleStatus.IN_PROGRESS;
        nextVersion();
    }

    /**
     * 记录单个样品项结论；非通过结论必须携带证据。
     */
    public void recordItemResult(
            String itemId,
            SampleDecision decision,
            List<Evidence> evidence
    ) {
        require(status == SampleStatus.IN_PROGRESS, "对样单不在进行中");
        require(decision != null && decision != SampleDecision.PENDING, "必须给出对样结论");
        require(
                decision == SampleDecision.PASSED || (evidence != null && !evidence.isEmpty()),
                "非通过结论必须提供证据"
        );
        requireItem(itemId).record(decision, evidence);
        nextVersion();
    }

    /**
     * 汇总所有样品项，按异常、未到样、通过的优先级形成整体结论。
     */
    public void complete() {
        require(status == SampleStatus.IN_PROGRESS, "对样单不在进行中");
        require(items.stream().allMatch(SampleItem::hasDecision), "仍有样品项未给出结论");
        if (items.stream().anyMatch(item -> item.decision() == SampleDecision.ABNORMAL)) {
            status = SampleStatus.ABNORMAL;
        } else if (items.stream().anyMatch(item -> item.decision() == SampleDecision.NOT_ARRIVED)) {
            status = SampleStatus.NOT_ARRIVED;
        } else {
            status = SampleStatus.PASSED;
        }
        long eventVersion = nextVersion();
        if (status == SampleStatus.PASSED) {
            raise(new QualityEvents.SampleMatchingPassed(
                    UUID.randomUUID().toString(),
                    id,
                    eventVersion,
                    Instant.now(),
                    reference.businessNo()
            ));
        } else {
            raise(new QualityEvents.SampleMatchingAbnormal(
                    UUID.randomUUID().toString(),
                    id,
                    eventVersion,
                    Instant.now(),
                    reference.businessNo(),
                    status.name()
            ));
        }
    }

    /** 查找目标对样项，不存在时拒绝命令。 */
    private SampleItem requireItem(String itemId) {
        return items.stream()
                .filter(item -> item.id().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("对样项不存在: " + itemId));
    }

    /** 返回对样聚合标识。 */
    public String id() {
        return id;
    }

    /** 返回对样单整体状态。 */
    public SampleStatus status() {
        return status;
    }

    /** 返回对样项只读副本。 */
    public List<SampleItem> items() {
        return List.copyOf(items);
    }

    public enum ReferenceScope {
        ORDER,
        ORDER_LINE,
        SHIPMENT
    }

    public enum SampleStatus {
        NOT_STARTED,
        IN_PROGRESS,
        PASSED,
        ABNORMAL,
        NOT_ARRIVED
    }

    public enum SampleDecision {
        PENDING,
        PASSED,
        ABNORMAL,
        NOT_ARRIVED
    }

    /** 单个 SKU 与其样品基准的对比结果。 */
    public static final class SampleItem {

        /** 对样项标识。 */
        private final String id;

        /** 被对比商品的 SKU 引用。 */
        private final SkuRef sku;

        /** 文件库或样品库中的基准版本引用。 */
        private final String sampleBaselineReference;

        /** 当前对样决定。 */
        private SampleDecision decision = SampleDecision.PENDING;

        /** 支撑结论的图片、报告等文件引用。 */
        private List<Evidence> evidence = List.of();

        /** 创建等待记录结论的对样项。 */
        public SampleItem(String id, SkuRef sku, String sampleBaselineReference) {
            require(id != null && !id.isBlank(), "对样项 ID 不能为空");
            require(sku != null, "SKU 不能为空");
            require(
                    sampleBaselineReference != null && !sampleBaselineReference.isBlank(),
                    "样品基准引用不能为空"
            );
            this.id = id;
            this.sku = sku;
            this.sampleBaselineReference = sampleBaselineReference;
        }

        /** 写入对样决定及其不可变证据列表。 */
        private void record(SampleDecision decision, List<Evidence> evidence) {
            this.decision = decision;
            this.evidence = evidence == null ? List.of() : List.copyOf(evidence);
        }

        /** 判断本项是否已经给出最终决定。 */
        private boolean hasDecision() {
            return decision != SampleDecision.PENDING;
        }

        /** 返回对样项标识。 */
        public String id() {
            return id;
        }

        /** 返回商品引用。 */
        public SkuRef sku() {
            return sku;
        }

        /** 返回样品基准版本引用。 */
        public String sampleBaselineReference() {
            return sampleBaselineReference;
        }

        /** 返回当前对样决定。 */
        public SampleDecision decision() {
            return decision;
        }

        /** 返回结论证据引用。 */
        public List<Evidence> evidence() {
            return evidence;
        }
    }

    /**
     * 质量结论证据。
     *
     * @param fileReference 文件存储引用，不保存二进制内容
     * @param note 证据说明
     */
    public record Evidence(String fileReference, String note) {

        /** 校验证据必须引用一个已保存文件。 */
        public Evidence {
            require(fileReference != null && !fileReference.isBlank(), "证据文件引用不能为空");
        }
    }
}
