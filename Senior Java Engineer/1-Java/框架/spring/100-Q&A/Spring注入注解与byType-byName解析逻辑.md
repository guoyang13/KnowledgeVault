# Spring 注入注解与 byType / byName 解析逻辑

> 导航：[[00-Spring-Bean加载-学习导航]] · **100-Q&A** · 依赖注入 · 源码解析
>
> 前置：[[20-依赖注入实现原理]] · [[100-Q&A/Spring依赖注入形式分类与Demo]]
>
> 源码：
> - `DefaultListableBeanFactory.doResolveDependency()` L1722–1841
> - `CommonAnnotationBeanPostProcessor.getResource()` / `autowireResource()`
> - `AbstractAutowireCapableBeanFactory.autowireByName()` / `autowireByType()`

---

## 一句话

`@Autowired` 在 `doResolveDependency` 里 **先尝试 byName（字段名/参数名/@Qualifier），再 byType**；`@Resource` **先 byName（name/字段名），默认名找不到才 byType 兜底**；XML `autowire` 则是纯粹的 byName 或 byType。

---

## 一、注解与处理器对照

| 注解 | 处理器（BPP） | 默认解析策略 | 注入阶段 |
|------|--------------|-------------|----------|
| `@Autowired` | `AutowiredAnnotationBeanPostProcessor` | **先 byName，再 byType** | 构造器 / populateBean |
| `@Inject` | 同上 | 同 `@Autowired` | 同上 |
| `@Value` | 同上 | 配置值 / SpEL，不 getBean | populateBean |
| `@Resource` | `CommonAnnotationBeanPostProcessor` | **先 byName，失败再 byType** | populateBean |
| `@Qualifier` | 辅助注解 | 提供 suggestedName | — |
| `@Primary` | 标记在 Bean 上 | byType 多候选时优先 | — |

---

## 二、`@Autowired` / `@Inject` 的解析逻辑

两者最终都走 **`DefaultListableBeanFactory.doResolveDependency()`**。

### 2.1 完整决策链（Step 1 → 6）

```text
doResolveDependency(descriptor, requestingBeanName)
│
├─ Step 1  shortcut（缓存过的字段，直接记了 beanName）
│
├─ Step 2  @Value → getSuggestedValue → 解析 ${} / SpEL
│
├─ Step 3  ★ byName 快捷路径
│            dependencyName = 字段名 / 参数名
│            若无 Bean，再试 @Qualifier 的 value
│            containsBean(name) && isTypeMatch(name, type)
│            → getBean(dependencyName)
│
├─ Step 4a 集合/数组/Map 注入
│
├─ Step 4b  ★ byType
│            findAutowireCandidates(beanName, type)
│            唯一候选 → resolveCandidate → getBean
│            多个候选 → Step 5
│
└─ Step 5  多候选消歧 determineAutowireCandidate
             ① @Primary
             ② 字段名匹配 beanName
             ③ @Qualifier value 匹配 beanName
             ④ @Priority
             ⑤ defaultCandidate
             仍无法唯一 → NoUniqueBeanDefinitionException
```

**源码位置：** `DefaultListableBeanFactory.java` L1722–1841

---

### 2.2 Step 3：byName（`@Autowired` 的隐式 byName）

```java
@Service
public class OrderService {
    @Autowired
    private UserRepository userRepository;  // 字段名 "userRepository"
}

@Repository("userRepository")
public class MysqlUserRepository implements UserRepository {}
```

```text
dependencyName = "userRepository"     // getDependencyName() = 字段名
containsBean("userRepository") == true
isTypeMatch("userRepository", UserRepository.class) == true
→ getBean("userRepository")             // Step 3 直接按名
```

> `@Autowired` **不是纯 byType**；字段名与 Bean 名一致时 Step 3 优先。

---

### 2.3 Step 4b + 5：byType + 消歧

```java
@Primary
@Component
public class AlipayClient implements PaymentClient {}

@Component
public class WechatPayClient implements PaymentClient {}

@Service
public class OrderService {
    @Autowired
    private PaymentClient paymentClient;  // 字段名 ≠ Bean 名
}
```

```text
Step 3: dependencyName="paymentClient"，containsBean? → false，跳过
Step 4b: findAutowireCandidates(PaymentClient.class) → 2 个候选
Step 5:  @Primary → alipayClient
→ getBean("alipayClient")
```

**`determineAutowireCandidate` 优先级（L2116–2159）：**

| 顺序 | 规则 |
|:----:|------|
| 1 | `@Primary` |
| 2 | 字段名 / 参数名匹配 beanName |
| 3 | `@Qualifier` 的 value 匹配 beanName |
| 4 | `@Priority` 最高 |
| 5 | defaultCandidate |

---

### 2.4 `@Qualifier` 的两个作用点

```java
@Autowired
@Qualifier("wechatPayClient")
private PaymentClient paymentClient;
```

| 阶段 | 作用 |
|------|------|
| Step 3 | `getSuggestedName()` → `"wechatPayClient"` → 按名 getBean |
| Step 5 | 多候选时按 Qualifier value 匹配 beanName |

**源码：** `QualifierAnnotationAutowireCandidateResolver.getSuggestedName()` L393–402

---

### 2.5 `@Inject` 与 `@Autowired` 差异

| | `@Autowired` | `@Inject` |
|--|-------------|-----------|
| 处理器 | `AutowiredAnnotationBeanPostProcessor` | 同上 |
| 解析链 | 相同 | 相同 |
| `required=false` | ✅ 支持 | ❌ 始终 required |

`@Inject` 无 `required` 属性 → `determineRequiredStatus` 默认 true。

---

## 三、`@Resource` 的解析逻辑

处理器：`CommonAnnotationBeanPostProcessor`

### 3.1 名称如何确定

```java
@Resource                          // 未指定 name
private UserRepository userRepository;
// name = "userRepository"，isDefaultName = true

@Resource(name = "mysqlRepo")
private UserRepository repo;
// name = "mysqlRepo"，isDefaultName = false

@Resource
public void setUserRepository(UserRepository r) {}
// setter：去掉 set 前缀 → "userRepository"
```

**源码：** `CommonAnnotationBeanPostProcessor.ResourceElement` L705–728

---

### 3.2 查找逻辑

```text
getResource(element)
  autowireResource(factory, element)
    │
    ├─ isDefaultName && !containsBean(name)
    │     → fallbackToDefaultTypeMatch（默认 true）
    │     → resolveDependency(byType)     ★ 按类型兜底
    │
    └─ 否则
          → resolveBeanByName(name)       ★ getBean(name, type)
```

**源码：** L588–612

```java
// resolveBeanByName = getBean(name, dependencyType)
public Object resolveBeanByName(String name, DependencyDescriptor descriptor) {
    return getBean(name, descriptor.getDependencyType());
}
```

---

### 3.3 Demo 对比

```java
// byName 成功
@Resource
private UserRepository mysqlUserRepository;
@Repository("mysqlUserRepository")
public class MysqlUserRepository implements UserRepository {}

// byName 失败 → byType 兜底
@Resource
private UserRepository repo;   // 名 "repo"，容器无此 Bean
@Repository
public class MysqlUserRepository implements UserRepository {}
// → resolveDependency(UserRepository.class) → @Primary 或报错
```

---

### 3.4 `@Autowired` vs `@Resource`

| | `@Autowired` | `@Resource` |
|--|-------------|-------------|
| 第一策略 | 字段名作 byName 尝试（Step 3） | **强制** `resolveBeanByName` |
| 名称不对 | 直接 byType（Step 4b） | 仅 `isDefaultName` 才 byType 兜底 |
| 显式指定 | `@Qualifier` | `@Resource(name="...")` |
| 多候选 | @Primary / @Qualifier | 兜底走 byType 同一套 |

---

## 四、XML autowire

在 `populateBean` 里、BPP **之前**执行（L1458–1468）。

### 4.1 `autowire="byName"`

```xml
<bean id="orderService" class="com.example.OrderService" autowire="byName"/>
```

```text
autowireByName()                          // L1508–1528
  遍历 writable 属性（unsatisfiedNonSimpleProperties）
  对每个 propertyName：
    containsBean(propertyName)?
      yes → getBean(propertyName)
      no  → 跳过（不报错）
```

**纯 byName：** 属性名 = Bean id，不看类型（setter 类型仍需兼容）。

---

### 4.2 `autowire="byType"`

```xml
<bean id="orderService" class="com.example.OrderService" autowire="byType"/>
```

```text
autowireByType()                          // L1542–1579
  遍历 writable 属性
  取 setter 参数类型
  → resolveDependency(desc)               ★ 与 @Autowired byType 相同
```

**纯 byType：** 按 setter 参数类型，**不看属性名**；多个同类型 → 报错。

---

### 4.3 对比 Demo

```java
@Repository("mysqlRepo")
public class MysqlUserRepository implements UserRepository {}

@Repository("redisRepo")
public class RedisUserRepository implements UserRepository {}

public class OrderService {
    private UserRepository userRepository;
    public void setUserRepository(UserRepository r) { this.userRepository = r; }
}
```

| autowire | 结果 |
|----------|------|
| **byName** | 找 id=`userRepository` → **找不到，不注入** |
| **byType** | 找 `UserRepository` → **2 候选，报错** |
| **byName + 改属性名** | 属性改 `mysqlRepo` → ✅ |

---

## 五、三种 byName / byType 对比总表

| 机制 | 触发方式 | Name 来源 | Type 作用 | 多候选 |
|------|----------|-----------|-----------|--------|
| `@Autowired` Step3 | 字段/参数名 | 字段名、参数名 | 校验 `isTypeMatch` | — |
| `@Autowired` Step4b | 类型 | 辅助消歧 | `findAutowireCandidates` | @Primary / @Qualifier |
| `@Resource` | 显式/默认名 | `name` 或字段名 | 仅兜底 | 兜底走 byType |
| XML **byName** | 属性名 | JavaBean 属性名 | 无 | 按 id 唯一 |
| XML **byType** | setter 类型 | 不参与 | setter 参数类型 | 报错 |

---

## 六、综合 Demo

```java
public interface Cache { void put(String k, Object v); }

@Primary
@Component("redisCache")
public class RedisCache implements Cache {
    public void put(String k, Object v) {}
}

@Component("localCache")
public class LocalCache implements Cache {
    public void put(String k, Object v) {}
}

@Service
public class DemoService {

    // @Autowired byName：字段名 = Bean 名
    @Autowired
    private Cache redisCache;              // Step3 → getBean("redisCache")

    // @Autowired byType + @Primary
    @Autowired
    private Cache defaultCache;            // Step4b → redisCache(@Primary)

    // @Autowired + @Qualifier
    @Autowired @Qualifier("localCache")
    private Cache local;                   // Step3 → getBean("localCache")

    // @Resource byName
    @Resource(name = "localCache")
    private Cache resourceByName;          // resolveBeanByName

    // @Resource 默认名不匹配 → byType 兜底
    @Resource
    private Cache cache;                   // name="cache" 不存在 → byType → @Primary

    @Value("${app.mode:dev}")
    private String mode;

    public DemoService(@Qualifier("localCache") Cache c) {}
}
```

---

## 七、构造器注入的 byName / byType

构造器参数同样走 `doResolveDependency`：

```java
public OrderService(UserRepository userRepository,           // 参数名 byName
                    @Qualifier("mysqlRepo") UserRepository r)
```

- 编译带 `-parameters`：参数名可用于 Step 3
- 无参数名：直接 Step 4b byType

---

## 八、源码行号地图

| 步骤 | 类 | 行号 |
|------|-----|------|
| `doResolveDependency` 主流程 | `DefaultListableBeanFactory` | L1722–1841 |
| Step3 byName | `DefaultListableBeanFactory` | L1757–1777 |
| Step4b byType | `DefaultListableBeanFactory` | L1788–1836 |
| 多候选消歧 | `DefaultListableBeanFactory.determineAutowireCandidate` | L2116–2159 |
| `@Qualifier` suggestedName | `QualifierAnnotationAutowireCandidateResolver` | L393–402 |
| `resolveCandidate` → getBean | `DependencyDescriptor` | L251–255 |
| `@Resource` autowireResource | `CommonAnnotationBeanPostProcessor` | L588–612 |
| XML autowireByName | `AbstractAutowireCapableBeanFactory` | L1508–1528 |
| XML autowireByType | `AbstractAutowireCapableBeanFactory` | L1542–1579 |

---

## 九、记忆口诀

```text
@Autowired  → 先名字（字段/参数/@Qualifier），再类型，多候选看 @Primary
@Resource   → 先名字（name/字段名），默认名找不到才 byType 兜底
@Value      → 只读配置，不 getBean
XML byName  → 属性名 = Bean id
XML byType  → setter 参数类型，多个同类型报错
```

---

## 关联

- [[100-Q&A/Spring依赖注入形式分类与Demo]]
- [[20-依赖注入实现原理]]
- [[21-循环依赖与三级缓存详解]]
- [[12-扩展点层-BeanPostProcessor详解]]
