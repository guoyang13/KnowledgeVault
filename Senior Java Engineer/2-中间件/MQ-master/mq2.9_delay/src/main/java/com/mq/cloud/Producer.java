package com.mq.cloud;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.common.message.Message;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * <h1>8位技术专家全程教学实战，全面覆盖Java进阶知识体系。<br/><br/><a href="https://u.geekbang.org/subject/java4th/1001148?source=app_share">极客训练营地址：https://u.geekbang.org/subject/java4th/1001148?source=app_share</a></h1><br/><h1><a href="https://gitee.com/ylimhhmily/MQ">极客案例代码地址：https://gitee.com/ylimhhmily/MQ</a></h1><br/><h1><a href="https://time.geekbang.org/column/intro/100312101">Dubbo 源码剖析与实战：https://time.geekbang.org/column/intro/100312101</a></h1>
 *
 * @author hmilyylimh
 * ^_^
 * @version 0.0.1
 * ^_^
 * @date 2023-06-18
 */
public class Producer {

    public static void main(String[] args) throws Exception {
        // 1、创建生产者对象
        DefaultMQProducer producer = new DefaultMQProducer("producer_group");

        // 2、为生产者对象设置 NameServer 地址
        producer.setNamesrvAddr("127.0.0.1:9876");

        // 3、把我们的生产者直接启动起来
        producer.start();

        // 4、创建消息、并发送消息
        for (int i = 0; i < 3; i++) {
            // public Message(String topic, String tags, String keys, byte[] body) {
            Message message = new Message(
                    "custom-delay-topic",
                    "delayTag",
                    "CUSTOM_DELAY",
                    ("("+i+")Hello Message From Delay Producer, " +
                            "date="+new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date())).getBytes()
            );
            // 设置定时的逻辑
            // "1s 5s 10s 30s 1m 2m 3m 4m 5m 6m 7m 8m 9m 10m 20m 30m 1h 2h";
            message.setDelayTimeLevel(2);

            // 利用生产者对象，将消息直接发送出去
            producer.send(message);
        }
        System.out.println("Send Finished.");
    }
}
