---
aliases:
  - DDD Java落地代码骨架
tags:
  - DDD
  - Java
  - 代码骨架
---

# Java落地代码骨架

> 目标：把 DDD 概念映射到 Java 工程结构，避免停留在术语层面。

## 推荐包结构

以订单上下文为例：

```text
com.example.trade.order
  interfaces
    rest
      OrderController
      request
      response
  application
    OrderApplicationService
    command
      SubmitOrderCommand
      PayOrderCommand
    dto
      OrderDetailDTO
  domain
    model
      Order
      OrderId
      OrderItem
      OrderStatus
      OrderSubmitted
      OrderPaid
      OrderRepository
    service
      PricingService
    event
      DomainEvent
      DomainEventPublisher
  infrastructure
    persistence
      MyBatisOrderRepository
      OrderMapper
      OrderDO
    messaging
      SpringDomainEventPublisher
    acl
      ErpOrderClient
      ErpOrderTranslator
```

## 值对象

```java
public record OrderId(String value) {
    public OrderId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("order id is required");
        }
    }
}
```

```java
public record Money(BigDecimal amount, String currency) {
    public Money {
        if (amount == null || currency == null) {
            throw new IllegalArgumentException("money is incomplete");
        }
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("amount must be non-negative");
        }
    }

    public Money add(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("currency mismatch");
        }
        return new Money(amount.add(other.amount), currency);
    }
}
```

## 实体和聚合根

```java
public class Order {
    private final OrderId id;
    private final List<OrderItem> items;
    private OrderStatus status;
    private final List<DomainEvent> events = new ArrayList<>();

    private Order(OrderId id, List<OrderItem> items) {
        if (items == null || items.isEmpty()) {
            throw new IllegalArgumentException("order must have items");
        }
        this.id = id;
        this.items = new ArrayList<>(items);
        this.status = OrderStatus.DRAFT;
    }

    public static Order create(OrderId id, List<OrderItem> items) {
        return new Order(id, items);
    }

    public void submit() {
        if (status != OrderStatus.DRAFT) {
            throw new IllegalStateException("only draft order can be submitted");
        }
        this.status = OrderStatus.SUBMITTED;
        this.events.add(new OrderSubmitted(id, Instant.now()));
    }

    public void markPaid() {
        if (status != OrderStatus.SUBMITTED) {
            throw new IllegalStateException("only submitted order can be paid");
        }
        this.status = OrderStatus.PAID;
        this.events.add(new OrderPaid(id, Instant.now()));
    }

    public List<DomainEvent> pullEvents() {
        List<DomainEvent> pulled = List.copyOf(events);
        events.clear();
        return pulled;
    }
}
```

## 领域事件

```java
public interface DomainEvent {
    Instant occurredAt();
}
```

```java
public record OrderSubmitted(
    OrderId orderId,
    Instant occurredAt
) implements DomainEvent {
}
```

## Repository接口

Repository 接口属于领域层，表达模型需要的集合式访问。

```java
public interface OrderRepository {
    Optional<Order> findById(OrderId id);
    void save(Order order);
}
```

实现属于基础设施层：

```java
public class MyBatisOrderRepository implements OrderRepository {
    private final OrderMapper mapper;

    @Override
    public Optional<Order> findById(OrderId id) {
        OrderDO orderDO = mapper.selectById(id.value());
        return Optional.ofNullable(orderDO).map(this::toDomain);
    }

    @Override
    public void save(Order order) {
        mapper.upsert(toDataObject(order));
    }
}
```

## 应用服务

```java
public class OrderApplicationService {
    private final OrderRepository orderRepository;
    private final DomainEventPublisher eventPublisher;

    public void submit(SubmitOrderCommand command) {
        Order order = orderRepository.findById(new OrderId(command.orderId()))
            .orElseThrow(() -> new IllegalArgumentException("order not found"));

        order.submit();
        orderRepository.save(order);
        eventPublisher.publish(order.pullEvents());
    }
}
```

应用服务要薄。它负责编排，不负责判断“订单什么状态可以提交”这类核心规则。

## 防腐层

外部系统模型不要直接进入领域层。

```java
public class ErpOrderTranslator {
    public ExternalOrderSnapshot translate(ErpOrderDTO dto) {
        return new ExternalOrderSnapshot(
            dto.getOrderNo(),
            dto.getBuyerCode(),
            dto.getTotalAmount()
        );
    }
}
```

这里的 `ExternalOrderSnapshot` 仍然要用本地语言表达，不要把 ERP 字段名一路传到领域模型里。

## 单元测试重点

领域模型测试不依赖 Spring，不依赖数据库。

```java
class OrderTest {
    @Test
    void draft_order_can_be_submitted() {
        Order order = Order.create(new OrderId("O-1"), List.of(sampleItem()));

        order.submit();

        assertThat(order.pullEvents()).hasOnlyElementsOfType(OrderSubmitted.class);
    }
}
```

优先测试：

- 聚合不变量。
- 状态转换规则。
- 值对象校验。
- 领域事件是否产生。
- 领域服务策略。

## 代码审查清单

- Controller 是否直接操作 Repository？
- 应用服务是否有大量业务 `if/else`？
- 领域对象是否只有 getter/setter？
- Repository 是否暴露数据库细节？
- 外部 DTO 是否进入领域层？
- 聚合之间是否互相持有完整对象？
- 一次事务是否修改多个聚合？
- 包名是否表达通用语言？

## 关联

- [[05-第05章-实体]]
- [[06-第06章-值对象]]
- [[08-第08章-领域事件]]
- [[10-第10章-聚合]]
- [[12-第12章-资源库]]
- [[14-第14章-应用程序]]
- [[01-DDD落地练习-以项目管理SaaS为例]]
