package com.mq.cloud;

import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * <h1>8位技术专家全程教学实战，全面覆盖Java进阶知识体系。<br/><br/><a href="https://u.geekbang.org/subject/java4th/1001148?source=app_share">极客训练营地址：https://u.geekbang.org/subject/java4th/1001148?source=app_share</a></h1><br/><h1><a href="https://gitee.com/ylimhhmily/MQ">极客案例代码地址：https://gitee.com/ylimhhmily/MQ</a></h1><br/><h1><a href="https://time.geekbang.org/column/intro/100312101">Dubbo 源码剖析与实战：https://time.geekbang.org/column/intro/100312101</a></h1>
 *
 * @author hmilyylimh
 * ^_^
 * @version 0.0.1
 * ^_^
 * @date 2023-06-18
 */
public class Consumer {

    public static void main(String[] args) throws Exception {
        // 1、创建消费者对象
        DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("batch_group");

        // 2、为消费者对象设置 NameServer 地址
        consumer.setNamesrvAddr("127.0.0.1:9876");

        // 3、订阅主题
        consumer.subscribe("custom-batch-topic", "*");

        // 4、注册监听消息，并打印消息
        consumer.registerMessageListener(new MessageListenerConcurrently() {
            @Override
            public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> msgs,
                                                            ConsumeConcurrentlyContext context) {
                for (MessageExt msg : msgs) {
                    String printMsg = new String(msg.getBody()) + ", recvTime: "
                            + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
                    System.out.println(printMsg);
                }

                return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
            }
        });

        // 5、把消费者直接启动起来
        consumer.start();
        System.out.println("Consumer Started Finished.");
    }
}
