package com.mq.cloud;

import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * RocketMQ 事务消息 Demo：本地写库 + 发消息最终一致。
 *
 * <p>运行前：NameServer(9876) + Broker 已启动。
 */
public class OrderTransactionProducerDemo {

    private static final String TOPIC = "Transaction-Test-Topic";
    private static final String PRODUCER_GROUP = "order_tx_producer_group";

    public static void main(String[] args) throws Exception {
        TransactionMQProducer producer = new TransactionMQProducer(PRODUCER_GROUP);
        producer.setNamesrvAddr("127.0.0.1:9876");

        ExecutorService executor = new ThreadPoolExecutor(
                2, 5, 100, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2000),
                r -> {
                    Thread t = new Thread(r, "client-transaction-msg-check-thread");
                    t.setDaemon(true);
                    return t;
                });
        producer.setExecutorService(executor);

        producer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                String txId = msg.getKeys();
                String payload = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.printf("[executeLocalTransaction] txId=%s%n", txId);

                boolean ok = LocalOrderService.createOrder(txId, payload);
                if (ok) {
                    return LocalTransactionState.COMMIT_MESSAGE;
                }
                return LocalTransactionState.ROLLBACK_MESSAGE;
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                String txId = msg.getKeys();
                LocalOrderService.TxStatus status = LocalOrderService.getStatus(txId);
                System.out.printf("[checkLocalTransaction] txId=%s, status=%s%n", txId, status);

                switch (status) {
                    case COMMITTED:
                        return LocalTransactionState.COMMIT_MESSAGE;
                    case ROLLBACK:
                        return LocalTransactionState.ROLLBACK_MESSAGE;
                    case PENDING:
                    default:
                        return LocalTransactionState.UNKNOW;
                }
            }
        });

        producer.start();

        String txId = UUID.randomUUID().toString().replace("-", "");
        Message message = new Message(
                TOPIC,
                "OrderCreated",
                ("orderId=10001,amount=99.00").getBytes(StandardCharsets.UTF_8));
        message.setKeys(txId);

        producer.sendMessageInTransaction(message, null);
        System.out.println("Half message sent, waiting for local tx + commit...");

        Thread.sleep(5000);
        producer.shutdown();
        executor.shutdown();
    }
}
