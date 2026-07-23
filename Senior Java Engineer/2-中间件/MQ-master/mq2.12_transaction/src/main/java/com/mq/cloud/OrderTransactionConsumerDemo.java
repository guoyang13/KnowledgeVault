package com.mq.cloud;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.nio.charset.StandardCharsets;

/**
 * 消费事务消息 Commit 后投递的业务消息。
 */
public class OrderTransactionConsumerDemo {

    private static final String TOPIC = "Transaction-Test-Topic";
    private static final String CONSUMER_GROUP = "order_tx_consumer_group";

    public static void main(String[] args) throws Exception {
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer(CONSUMER_GROUP);
        consumer.setNamesrvAddr("127.0.0.1:9876");
        consumer.subscribe(TOPIC, "*");

        consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
            for (MessageExt msg : msgs) {
                String body = new String(msg.getBody(), StandardCharsets.UTF_8);
                System.out.printf("[Consumer] txKey=%s, tag=%s, body=%s%n",
                        msg.getKeys(), msg.getTags(), body);
            }
            return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
        });

        consumer.start();
        System.out.println("OrderTransactionConsumerDemo started.");
    }
}
