package com.bo.rt.biz.scm.purchaseorder.next.application.shared.port;

/**
 * 由基础设施实现 UUID、雪花 ID 或公司统一 ID 服务。
 */
public interface IdentityGenerator {

    /** 为指定领域对象类型生成全局唯一标识。 */
    String nextId(String type);
}
