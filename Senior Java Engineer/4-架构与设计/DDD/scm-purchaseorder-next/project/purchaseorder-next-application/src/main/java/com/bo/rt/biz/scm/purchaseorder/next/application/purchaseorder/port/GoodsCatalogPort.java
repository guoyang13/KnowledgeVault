package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.port;

/**
 * 商品目录端口。
 *
 * <p>用于查询 SKU 是否存在、是否可采购，以及补齐商品基础信息。</p>
 */
public interface GoodsCatalogPort {

    /**
     * 判断 SKU 是否允许采购。
     *
     * @param skuCode SKU 编码
     * @return 允许采购时返回 true，否则返回 false
     */
    boolean canPurchase(String skuCode);
}
