package com.mq.cloud;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.ArrayList;
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
public class PartlyProducer {

    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer(
                "producer_group",
                true);
        producer.setNamesrvAddr("127.0.0.1:9876");
        producer.start();

        List<Order> list1 = buildOrderList(1, "Buy", "Pay", "Finish");
        List<Order> list2 = buildOrderList(2, "Buy", "Pay");
        List<Order> list3 = buildOrderList(3, "Pay", "Finish");

        List<Order> allList = new ArrayList<>(9);
        allList.addAll(list1);
        allList.addAll(list2);
        allList.addAll(list3);

        int size = allList.size();
        for (int i = 0; i < size; i++) {
            Order order = allList.get(i);
            Message msg = new Message(
                    "Partly-Orderly-Topic",
                    "Partly_Orderly_Tag",
                    (order.toString()).getBytes());
            msg.setKeys("Partly_Orderly_Tag");

            producer.send(msg, new MessageQueueSelector() {
                @Override
                public MessageQueue select(List<MessageQueue> mqs, Message msg, Object arg) {
                    int orderId = (int)arg;
                    int idx = orderId % mqs.size();
                    return mqs.get(idx);
                }
            }, order.orderId);
        }
        System.out.println("Send Finished.");
    }

    public static List<Order> buildOrderList(int orderId, String... descList) {
        List<Order> resultList = new ArrayList<>(3);

        for (String s : descList) {
            Order order = new Order();
            order.orderId = orderId;
            order.desc = s;

            resultList.add(order);
        }

        return resultList;
    }

    public static class Order {
        int orderId;
        String desc;

        @Override
        public String toString() {
            return "orderId="+orderId+", desc="+desc;
        }
    }
}