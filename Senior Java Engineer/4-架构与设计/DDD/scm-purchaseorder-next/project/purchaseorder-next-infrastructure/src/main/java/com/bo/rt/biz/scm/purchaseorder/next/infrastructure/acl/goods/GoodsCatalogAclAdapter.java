package com.bo.rt.biz.scm.purchaseorder.next.infrastructure.acl.goods;

import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port.GoodsCatalogPort;
import java.util.function.Predicate;

/**
 * 商品服务防腐层适配器。
 *
 * <p>负责把商品服务 DTO 翻译为采购订单上下文可理解的概念。</p>
 */
public class GoodsCatalogAclAdapter implements GoodsCatalogPort {

    /** 远程商品服务的“可采购”调用函数。 */
    private final Predicate<String> remoteCanPurchase;

    /** 注入远程商品服务调用实现。 */
    public GoodsCatalogAclAdapter(Predicate<String> remoteCanPurchase) {
        this.remoteCanPurchase = remoteCanPurchase;
    }

    /**
     * 判断 SKU 是否允许采购。
     *
     * @param skuCode SKU 编码
     * @return 允许采购时返回 true，否则返回 false
     */
    @Override
    public boolean canPurchase(String skuCode) {
        return remoteCanPurchase.test(skuCode);
    }
}
