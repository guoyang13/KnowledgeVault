---
type: canonical
status: reviewed
topic: Spring type conversion / data binding
source_version: 6.2.x
aliases:
  - Spring ConversionService
  - Spring 类型转换与数据绑定
---

# ConversionService 与 DataBinder 的边界

## 摘要

类型转换（Type Conversion）回答“一个值如何变成目标类型”；数据绑定（Data Binding）回答“一组外部属性如何写入对象属性并收集错误”。`ConversionService` 是通用转换门面，`PropertyEditor` 是较早的 JavaBeans 扩展机制，`DataBinder` 在转换之上组织属性访问、允许字段、校验和错误结果。

## 1. 核心类型

| API | 中文定位 | 主要职责 |
| --- | --- | --- |
| `Converter<S,T>` | 单向类型转换器 | 一个源类型到目标类型 |
| `ConverterFactory<S,R>` | 转换器工厂 | 为一组相关目标类型创建转换器 |
| `GenericConverter` | 泛型转换器 | 根据更丰富的类型描述动态转换 |
| `ConversionService` | 转换服务 | 查询可转换性并执行转换 |
| `Formatter<T>` | 格式化器 | 面向展示层的字符串解析与打印 |
| `PropertyEditor` | JavaBeans 属性编辑器 | 有状态的字符串/属性转换 |
| `DataBinder` | 数据绑定器 | 绑定属性、转换、校验、记录错误 |
| `BindingResult` | 绑定结果 | 保存字段值与绑定/校验错误 |

## 2. 三层关系

```text
ConversionService
  -> 通用、通常无状态、可全局复用的类型转换

PropertyEditor
  -> JavaBeans 历史机制，实例有状态，通常按绑定过程注册

DataBinder
  -> 调用 ConversionService / PropertyEditor
  -> 把输入属性写入目标对象
  -> 执行字段限制与 Validator
  -> 产生 BindingResult
```

`DataBinder` 不是另一个 `ConversionService`；它是更高层的对象绑定工作流。

## 3. 在 IoC 容器中的位置

Bean 创建期间，属性值可能先是字符串或配置对象，最终要转换成字段、Setter 参数或构造参数所需类型。`BeanWrapper`、`TypeConverter`、转换服务和属性编辑器共同参与这个过程。

```text
BeanDefinition property value
  -> BeanWrapper / TypeConverter
  -> ConversionService 或 PropertyEditor
  -> 目标属性类型
  -> 写入 Bean
```

依赖解析还会使用 `ResolvableType` 保留泛型信息；“寻找哪个 Bean”与“把某个值转成什么类型”是两个不同问题。

## 4. 如何选择扩展方式

| 需求 | 首选 |
| --- | --- |
| 应用全局、类型到类型转换 | `Converter` + `ConversionService` |
| 字符串的本地化解析和展示 | `Formatter` |
| 兼容旧 JavaBeans 或特定框架 API | `PropertyEditor` |
| 把请求/属性集合绑定到对象并校验 | `DataBinder` |

```java
final class StringToOrderId implements Converter<String, OrderId> {
    @Override
    public OrderId convert(String source) {
        return new OrderId(Long.parseLong(source));
    }
}
```

转换器应尽量保持确定性和无状态，错误信息要能说明源值、源类型与目标类型。

## 5. DataBinder 的安全边界

数据绑定会根据属性路径写入对象。处理不可信输入时，应明确：

- 允许或禁止绑定的字段；
- 是否允许自动增长嵌套路径和集合；
- 是否需要直接字段访问；
- 转换失败与校验失败如何返回；
- 领域对象是否应该直接成为外部输入绑定目标。

Web 层通常应使用专门输入模型，再显式转换为领域命令，避免外部字段直接修改不应暴露的领域状态。

## 6. 常见误区

| 误区 | 修正 |
| --- | --- |
| 类型兼容就不需要转换 | 字符串、数字、枚举、日期和自定义值对象常需要显式转换 |
| `PropertyEditor` 可作为无状态全局单例随意复用 | 它通常有状态，必须注意实例生命周期 |
| `DataBinder` 只负责类型转换 | 它还负责属性路径、字段限制、校验与错误收集 |
| Bean 候选解析就是类型转换 | 前者选择依赖对象，后者转换值表示 |

关联：[[4-doCreateBean深度解析]] · [[5-依赖注入实现原理]] · [[6-Context层-Environment与PropertySource详解]]
