package com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.service;

import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.command.CreatePurchaseOrderCommand;
import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.command.ImportPurchaseOrderCommand;
import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.result.ImportPurchaseOrderResult;
import com.bo.rt.biz.scm.purchaseorder.next.application.purchaseorder.result.PurchaseOrderResult;

/**
 * 采购订单应用服务。
 *
 * <p>该服务只负责编排用例：参数转换、外部端口校验、事务边界和领域事件发布。
 * 采购订单状态流转和核心规则应留在领域聚合中。</p>
 */
public class PurchaseOrderApplicationService {

    /**
     * 创建采购订单草稿。
     *
     * @param command 创建采购订单命令
     * @return 创建后的采购订单结果
     */
    public PurchaseOrderResult createDraft(CreatePurchaseOrderCommand command) {
        throw new UnsupportedOperationException("骨架工程仅表达应用用例边界，待补充业务实现。");
    }

    /**
     * 提交采购订单。
     *
     * @param purchaseOrderCode 采购订单号
     * @return 提交后的采购订单结果
     */
    public PurchaseOrderResult submit(String purchaseOrderCode) {
        throw new UnsupportedOperationException("骨架工程仅表达应用用例边界，待补充业务实现。");
    }

    /**
     * 导入采购订单。
     *
     * @param command 导入采购订单命令
     * @return 导入处理结果
     */
    public ImportPurchaseOrderResult importPurchaseOrders(ImportPurchaseOrderCommand command) {
        throw new UnsupportedOperationException("骨架工程仅表达导入用例边界，待补充业务实现。");
    }
}
