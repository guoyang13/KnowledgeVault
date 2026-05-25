package com.mq.cloud;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.MessageQueueSelector;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;

import java.text.DecimalFormat;
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
public class FilterProducer {

    public static void main(String[] args) throws Exception {
        DefaultMQProducer producer = new DefaultMQProducer(
                "producer_group",
                true);
        producer.setNamesrvAddr("127.0.0.1:9876");
        producer.start();

        List<Order> list1 = buildOrderList(1, 4,"Buy", "TG1");
        List<Order> list2 = buildOrderList(5, 4,"Pay", "TG2");
        List<Order> list3 = buildOrderList(9, 4,"Finish", "TG3");

        List<Order> allList = new ArrayList<>(12);
        allList.addAll(list1);
        allList.addAll(list2);
        allList.addAll(list3);

        int size = allList.size();
        for (int i = 0; i < size; i++) {
            Order order = allList.get(i);
            Message msg = new Message(
                    "Filter-Test-Topic",
                    order.tag,
                    (order.toString()).getBytes());
            msg.setKeys("Filter_Tag");
            msg.putUserProperty("idx", new DecimalFormat("00").format(order.orderId));

            // 直接将 msg 发送出去
            producer.send(msg);
        }
        System.out.println("Send Finished.");
    }

    public static List<Order> buildOrderList(int orderId, int size, String desc, String tag) {
        List<Order> resultList = new ArrayList<>(3);

        for (int i = 0; i < size; i++) {
            int newOrderId = orderId + i;

            Order order = new Order();
            order.orderId = newOrderId;
            order.desc = desc;
            order.tag = tag;

            resultList.add(order);
        }


        return resultList;
    }

    public static class Order {
        int orderId;
        String desc;
        String tag;

        @Override
        public String toString() {
            return "orderId="+orderId+", desc="+desc+", tag="+tag;
        }
    }
}