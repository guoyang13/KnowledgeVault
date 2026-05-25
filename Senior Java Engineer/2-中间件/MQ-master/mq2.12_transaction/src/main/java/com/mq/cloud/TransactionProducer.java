package com.mq.cloud;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.LocalTransactionState;
import org.apache.rocketmq.client.producer.TransactionListener;
import org.apache.rocketmq.client.producer.TransactionMQProducer;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageExt;

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
public class TransactionProducer {

    public static void main(String[] args) throws Exception {
        TransactionMQProducer producer = new TransactionMQProducer(
                "transaction_producer_group");
        producer.setNamesrvAddr("127.0.0.1:9876");

        producer.setTransactionListener(new TransactionListener() {
            @Override
            public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
                // 这里需要执行本地事务，如果本地事务成功的话，那么就返回成功
                // 如果本地事务失败的话，那么就返回失败，则回滚事务

                // 目前我这里仅仅只是向模拟 demo 而已，那么这里就暂时不做任何事情
                // 直接返回成功，就意味着本地事务操作成功
                // 直接返回失败，就以为者本地事务操作失败
                return LocalTransactionState.COMMIT_MESSAGE;
            }

            @Override
            public LocalTransactionState checkLocalTransaction(MessageExt msg) {
                // 触发事务的检查，提供给到生产者一个检查事务是否成功提交的机会

                return LocalTransactionState.COMMIT_MESSAGE;
            }
        });

        producer.start();

        List<Order> list1 = buildOrderList(1, 1,"Buy", "TG1");
        List<Order> allList = new ArrayList<>(12);
        allList.addAll(list1);

        int size = allList.size();
        for (int i = 0; i < size; i++) {
            Order order = allList.get(i);
            Message msg = new Message(
                    "Transaction-Test-Topic",
                    order.tag,
                    (order.toString()).getBytes());
            msg.setKeys("Transaction_Tag");
            msg.putUserProperty("idx", new DecimalFormat("00").format(order.orderId));

            // 直接将 msg 发送出去
            producer.sendMessageInTransaction(msg, null);
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