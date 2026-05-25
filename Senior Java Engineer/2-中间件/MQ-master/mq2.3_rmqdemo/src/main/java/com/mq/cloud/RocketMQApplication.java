package com.mq.cloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.cloud.stream.messaging.Source;

/**
 * <h1>8位技术专家全程教学实战，全面覆盖Java进阶知识体系。<br/><br/><a href="https://u.geekbang.org/subject/java4th/1001148?source=app_share">极客训练营地址：https://u.geekbang.org/subject/java4th/1001148?source=app_share</a></h1><br/><h1><a href="https://gitee.com/ylimhhmily/MQ">极客案例代码地址：https://gitee.com/ylimhhmily/MQ</a></h1><br/><h1><a href="https://time.geekbang.org/column/intro/100312101">Dubbo 源码剖析与实战：https://time.geekbang.org/column/intro/100312101</a></h1>
 *
 * @author hmilyylimh
 * ^_^
 * @version 0.0.1
 * ^_^
 * @date 2023-06-18
 */
@SpringBootApplication
@EnableBinding({ CustomSource.class, CustomSink.class })
public class RocketMQApplication {

    public static void main(String[] args) {
        SpringApplication.run(RocketMQApplication.class, args);
        System.out.println("【【【【【  RocketMQApplication 启动成功！！！   】】】】】");
    }

    @StreamListener("input")
    public void receiveInput(String receiveMsg) {
        System.out.println("input receive: " + receiveMsg);
    }

    @StreamListener("input2")
    public void receiveInput2(String receiveMsg) {
        System.out.println("input2 receive: " + receiveMsg);
    }
}