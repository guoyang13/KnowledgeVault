---
title: Spring MVC Controller 参数解析
tags:
  - Java
  - SpringMVC
  - ArgumentResolver
  - HttpMessageConverter
  - 源码分析
---

# 04 Controller 参数解析

[[03-HandlerMapping与HandlerAdapter|上一篇：HandlerMapping 与 HandlerAdapter]] · [[SpringMVC-HTTP请求处理全链路|返回索引]] · [[05-Controller返回值处理|下一篇：Controller 返回值处理]]

## 1. 示例方法

```java
public ResponseEntity<OrderVO> createOrder(
        @PathVariable Long userId,
        @RequestParam boolean notify,
        @RequestHeader("X-Request-Id") String requestId,
        @Valid @RequestBody CreateOrderRequest request) {
    // ...
}
```

Spring 最终需要生成：

```java
Object[] args = {
    42L,
    true,
    "req-10001",
    new CreateOrderRequest(1001L, 2)
};
```

## 2. 参数解析总入口

```java
ServletInvocableHandlerMethod#invokeAndHandle()

    InvocableHandlerMethod#invokeForRequest()                         // ★重要

        InvocableHandlerMethod#getMethodArgumentValues()              // ★核心
            // 遍历每一个MethodParameter

            HandlerMethodArgumentResolverComposite
                #getArgumentResolver(parameter)
                // 寻找supportsParameter()为true的解析器
                // 匹配结果会被缓存

            HandlerMethodArgumentResolverComposite
                #resolveArgument(parameter)
                // 委托具体Resolver生成参数

        InvocableHandlerMethod#doInvoke(args)
            // 参数准备完成后才真正调用Controller
```

最重要的分界：

```text
getMethodArgumentValues()
  HTTP请求 -> Java参数

doInvoke()
  进入业务Controller
```

## 3. 常见参数与解析器

| Controller 参数 | 典型解析器 |
| --- | --- |
| `@PathVariable` | `PathVariableMethodArgumentResolver` |
| `@RequestParam` | `RequestParamMethodArgumentResolver` |
| `@RequestHeader` | `RequestHeaderMethodArgumentResolver` |
| `@CookieValue` | `ServletCookieValueMethodArgumentResolver` |
| `@RequestBody` | `RequestResponseBodyMethodProcessor` |
| `@ModelAttribute`、未标注复杂对象 | `ServletModelAttributeMethodProcessor` |
| `BindingResult` | `ErrorsMethodArgumentResolver` |
| `RedirectAttributes` | `RedirectAttributesMethodArgumentResolver` |
| `HttpServletRequest/Response` | Servlet 类型参数解析器 |
| `Principal` | `PrincipalMethodArgumentResolver` |

## 4. `@PathVariable`

```java
PathVariableMethodArgumentResolver#resolveArgument()

    PathVariableMethodArgumentResolver#resolveName("userId")
        // 从URI模板变量Map取得字符串"42"

    WebDataBinder#convertIfNecessary("42", Long.class)
        // String转换成Long

    return 42L;
```

路径变量在 HandlerMapping 匹配路径时已经被提取并放入 request 属性。

## 5. `@RequestParam`

```java
RequestParamMethodArgumentResolver#resolveArgument()

    RequestParamMethodArgumentResolver#resolveName("notify")
        // request.getParameterValues("notify")

    WebDataBinder#convertIfNecessary("true", boolean.class)

    return true;
```

它可以读取：

- URL 查询参数；
- `application/x-www-form-urlencoded` 表单参数；
- multipart 参数。

## 6. `@RequestHeader`

```java
RequestHeaderMethodArgumentResolver#resolveArgument()

    RequestHeaderMethodArgumentResolver
        #resolveName("X-Request-Id")
        // 从请求头读取req-10001

    return "req-10001";
```

## 7. `@RequestBody` JSON 反序列化

```java
RequestResponseBodyMethodProcessor#resolveArgument()                  // ★核心

    RequestResponseBodyMethodProcessor#readWithMessageConverters()

        AbstractMessageConverterMethodArgumentResolver
            #readWithMessageConverters()

            contentType = application/json;

            for (HttpMessageConverter converter : converters) {
                converter#canRead(targetType, contentType);
            }

            ByteArrayHttpMessageConverter#canRead()
                // 不匹配CreateOrderRequest

            StringHttpMessageConverter#canRead()
                // 不匹配当前目标类型

            MappingJackson2HttpMessageConverter#canRead()
                // 支持CreateOrderRequest和application/json

            RequestBodyAdvice#beforeBodyRead()
                // 读取请求体前的扩展点

            MappingJackson2HttpMessageConverter#read()

                AbstractJackson2HttpMessageConverter#readJavaType()

                    ObjectMapper#reader()
                        #forType(CreateOrderRequest.class)

                    ObjectReader#readValue(requestInputStream)
                        // JSON -> CreateOrderRequest

            RequestBodyAdvice#afterBodyRead()
                // 反序列化完成后的扩展点
```

`Content-Type` 决定选择哪个转换器：

```text
application/json
  -> Jackson JSON转换器

text/plain
  -> String转换器

application/xml
  -> XML转换器，需要对应依赖
```

如果没有支持当前类型和 Content-Type 的转换器，通常抛出：

```text
HttpMediaTypeNotSupportedException
```

## 8. `@Valid` 校验

```text
JSON反序列化
  -> CreateOrderRequest
  -> WebDataBinderFactory.createBinder()
  -> validateIfApplicable()
  -> Validator.validate()
  -> BindingResult
```

校验失败通常抛出：

```text
MethodArgumentNotValidException
```

示例：

```java
public record CreateOrderRequest(
        @NotNull Long productId,
        @Min(1) int quantity) {
}
```

## 9. 表单对象与 `@ModelAttribute`

```java
@PostMapping("/users")
public String create(
        @ModelAttribute UserForm form,
        BindingResult bindingResult) {
    // ...
}
```

处理链：

```text
ServletModelAttributeMethodProcessor
  -> 创建UserForm
  -> WebDataBinder绑定请求参数
  -> 类型转换
  -> 执行@InitBinder配置
  -> 执行Validator
  -> 保存BindingResult
```

### `@InitBinder`

```java
@InitBinder
public void initBinder(WebDataBinder binder) {
    binder.setRequiredFields("name");
}
```

常用于：

- 设置必填字段；
- 注册类型转换器；
- 注册属性编辑器；
- 设置允许或禁止绑定的字段；
- 配置 Validator。

## 10. 真正调用 Controller

所有参数准备完成后：

```java
InvocableHandlerMethod#doInvoke(args)                                // ★核心

    Method method = getBridgedMethod();

    method#invoke(controllerBean, args)
        // 普通Java Controller最终调用点

        OrderController#createOrder(...)
            // 进入业务代码
```

Controller 抛出的 `InvocationTargetException` 会被拆包，真实业务异常继续交给 Spring MVC 异常处理链。

## 11. 请求体只能读取一次

Servlet 请求体是输入流，通常只能消费一次：

```text
日志Filter提前读取Body
  -> 输入流已消费
  -> @RequestBody可能读取不到数据
```

需要记录 Body 时应使用可缓存 request 包装器，并注意：

- 大请求体的内存占用；
- 敏感字段脱敏；
- 文件上传不要无条件缓存；
- 异步请求兼容性。

[[03-HandlerMapping与HandlerAdapter|上一篇：HandlerMapping 与 HandlerAdapter]] · [[SpringMVC-HTTP请求处理全链路|返回索引]] · [[05-Controller返回值处理|下一篇：Controller 返回值处理]]
