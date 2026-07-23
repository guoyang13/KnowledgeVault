package com.mq.cloud;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 模拟本地 DB：订单表 + 事务状态，供 executeLocalTransaction / checkLocalTransaction 使用。
 */
public final class LocalOrderService {

    public enum TxStatus {
        /** 本地事务进行中，回查时应返回 UNKNOW */
        PENDING,
        COMMITTED,
        ROLLBACK
    }

    private static final Map<String, TxStatus> TX_STATUS = new ConcurrentHashMap<>();

    private LocalOrderService() {
    }

    /** 模拟：在同一本地事务里创建订单并记录事务状态 */
    public static boolean createOrder(String txId, String orderPayload) {
        TX_STATUS.put(txId, TxStatus.PENDING);
        try {
            // 模拟 DB 写入；这里用 sleep 表示耗时，生产环境换成真实 JDBC/MyBatis
            Thread.sleep(100);
            System.out.printf("[LocalTx] order created, txId=%s, payload=%s%n", txId, orderPayload);
            TX_STATUS.put(txId, TxStatus.COMMITTED);
            return true;
        } catch (Exception e) {
            TX_STATUS.put(txId, TxStatus.ROLLBACK);
            return false;
        }
    }

    public static TxStatus getStatus(String txId) {
        return TX_STATUS.getOrDefault(txId, TxStatus.ROLLBACK);
    }
}
