package com.bo.rt.biz.scm.purchaseorder.next.interfaces.mq;

/**
 * WMS 入库回调消息消费者。
 *
 * <p>消息入口只负责幂等、协议转换和调用应用服务，不直接修改领域对象或数据库。</p>
 */
public class WmsInboundCallbackConsumer {

    /**
     * 处理 WMS 入库回调消息。
     *
     * @param messageBody 消息体
     */
    public void onMessage(String messageBody) {
        throw new UnsupportedOperationException("骨架工程仅表达 MQ 入口边界，待补充消息处理实现。");
    }
}
