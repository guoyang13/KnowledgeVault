package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.id;

import com.bo.rt.biz.scm.purchaseorder.next.application.shared.port.IdentityGenerator;
import java.util.UUID;

/** 使用对象类型前缀和 UUID 生成演示标识。 */
public class UuidIdentityGenerator implements IdentityGenerator {

    /** 为指定对象类型生成带可读前缀的 UUID。 */
    @Override
    public String nextId(String type) {
        return type + "-" + UUID.randomUUID();
    }
}
