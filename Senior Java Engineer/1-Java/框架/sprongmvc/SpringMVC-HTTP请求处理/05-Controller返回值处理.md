---
title: Spring MVC Controller 返回值处理
tags:
  - Java
  - SpringMVC
  - ReturnValueHandler
  - HttpMessageConverter
  - ViewResolver
  - 源码分析
---

# 05 Controller 返回值处理

[[04-Controller参数解析|上一篇：Controller 参数解析]] · [[SpringMVC-HTTP请求处理全链路|返回索引]] · [[06-异常处理与拦截器|下一篇：异常处理与拦截器]]

## 1. 统一入口

Controller 返回后：

```java
ServletInvocableHandlerMethod#invokeAndHandle()

    Object returnValue =
            InvocableHandlerMethod#invokeForRequest();

    HandlerMethodReturnValueHandlerComposite
        #handleReturnValue(returnValue)                               // ★核心

        HandlerMethodReturnValueHandlerComposite
            #selectHandler(returnValue, returnType)
            // 寻找supportsReturnType()为true的处理器

        handler#handleReturnValue(
                returnValue,
                returnType,
                mavContainer,
                webRequest)
```

返回值处理器决定：

- 返回值是否是响应体；
- 是否需要状态码和响应头；
- 是否是逻辑视图名；
- 是否是重定向；
- 是否启动异步处理；
- 请求是否已经处理完成。

## 2. `ResponseEntity`

示例：

```java
return ResponseEntity
        .status(HttpStatus.CREATED)
        .header("X-Order-Id", "9001")
        .body(orderVO);
```

处理器：

```text
HttpEntityMethodProcessor
```

调用链：

```java
HandlerMethodReturnValueHandlerComposite
    #handleReturnValue(responseEntity)

    HttpEntityMethodProcessor#supportsReturnType()
        // 支持ResponseEntity

    HttpEntityMethodProcessor#handleReturnValue()                     // ★核心

        ModelAndViewContainer#setRequestHandled(true)
            // 直接写响应，不使用ViewResolver

        HttpServletResponse#setStatus(201)

        responseHeaders#putAll(entityHeaders)

        writeWithMessageConverters(orderVO, ...)
```

`ResponseEntity` 可以控制：

- HTTP 状态码；
- HTTP 响应头；
- HTTP 响应体。

## 3. `@ResponseBody`

```java
@GetMapping("/orders/{id}")
@ResponseBody
public OrderVO getOrder(@PathVariable Long id) {
    return orderService.find(id);
}
```

处理器：

```text
RequestResponseBodyMethodProcessor
```

调用链：

```java
RequestResponseBodyMethodProcessor#handleReturnValue()

    ModelAndViewContainer#setRequestHandled(true)

    AbstractMessageConverterMethodProcessor
        #writeWithMessageConverters(returnValue, ...)

    // 不进入ViewResolver
```

`@RestController` 等价于：

```text
@Controller
  +
类级别的@ResponseBody
```

## 4. 内容协商与 JSON 序列化

示例请求头：

```http
Accept: application/json
```

示例映射：

```java
@PostMapping(produces = "application/json")
```

处理链：

```java
AbstractMessageConverterMethodProcessor
    #writeWithMessageConverters(orderVO, returnType, ...)

    // 计算可生成的媒体类型
    // 读取客户端Accept
    // 与Controller的produces取交集
    // 选择最终MediaType

    for (HttpMessageConverter converter : converters) {
        converter#canWrite(OrderVO.class, application/json);
    }

    MappingJackson2HttpMessageConverter#canWrite()
        // 匹配成功

    ResponseBodyAdvice#beforeBodyWrite()
        // 写响应体之前的扩展点

    MappingJackson2HttpMessageConverter#write(orderVO)

        AbstractJackson2HttpMessageConverter#writeInternal(orderVO)

            ObjectWriter#writeValue(
                    responseOutputStream,
                    orderVO)
                // Java对象 -> JSON
```

如果无法满足 `Accept`，通常抛出：

```text
HttpMediaTypeNotAcceptableException
  -> 406 Not Acceptable
```

### `Content-Type` 与 `Accept`

```text
Content-Type
  请求体是什么格式
  影响读取请求体

Accept
  客户端希望响应是什么格式
  影响写响应体
```

## 5. `ResponseBodyAdvice`

```java
ResponseBodyAdvice#beforeBodyWrite()
```

它位于转换器真正写出响应之前，常用于：

- 统一响应结构；
- 字段脱敏；
- 添加公共字段；
- 对特定响应进行转换。

使用时要避免：

- 重复包装已经统一的响应对象；
- 错误包装文件下载或字节流；
- 修改与声明 Content-Type 不兼容的内容；
- 在 Advice 中执行耗时业务。

## 6. 逻辑视图名

```java
@Controller
public class OrderPageController {

    @GetMapping("/orders")
    public String list(Model model) {
        model.addAttribute("orders", orderService.findAll());
        return "orders/list";
    }
}
```

调用链：

```java
HandlerMethodReturnValueHandlerComposite
    #handleReturnValue("orders/list")

    ViewNameMethodReturnValueHandler#handleReturnValue()
        // 把字符串放入ModelAndViewContainer作为视图名

RequestMappingHandlerAdapter#getModelAndView()
    // 创建ModelAndView

DispatcherServlet#processDispatchResult()

    DispatcherServlet#render(modelAndView, request, response)

        ViewResolver#resolveViewName("orders/list", locale)

        View#render(model, request, response)
            // 生成HTML响应
```

## 7. `redirect:`

```java
return "redirect:/messages/{id}";
```

调用链：

```java
ViewNameMethodReturnValueHandler#handleReturnValue()
    // 识别redirect:前缀
    // 保存逻辑视图名

RequestMappingHandlerAdapter#getModelAndView()

DispatcherServlet#render()

    ViewResolver#resolveViewName()

    RedirectView#render()
        // 展开URI模板变量
        // 追加合适的模型属性为查询参数
        // 保存FlashMap
        // 生成3xx响应
```

### 普通属性与 Flash 属性

```java
attributes.addAttribute("id", "1");
attributes.addAttribute("name", "value");
attributes.addFlashAttribute("successMessage", "yay!");
```

结果：

```text
id
  -> 展开/messages/{id}

name
  -> 追加为?name=value

successMessage
  -> 保存在FlashMap
  -> 不出现在URL
  -> 下一次请求可读取
```

## 8. String 返回值为什么有两种含义？

```text
@Controller
  返回String
  -> 通常是视图名

@RestController
  返回String
  -> 通常是响应体

@Controller + @ResponseBody
  返回String
  -> 响应体
```

决定因素不是 Java 类型本身，而是方法和类上的注解，以及最终选中的返回值处理器。

## 9. REST 与页面渲染的分叉点

```text
共同主链路
DispatcherServlet
  -> HandlerMapping
  -> RequestMappingHandlerAdapter
  -> 参数解析
  -> Controller
        |
        +-- ResponseEntity / @ResponseBody
        |     -> ReturnValueHandler
        |     -> HttpMessageConverter
        |     -> JSON/XML/文本
        |
        +-- String / ModelAndView
              -> ModelAndView
              -> ViewResolver
              -> View.render()
              -> HTML或重定向
```

真正的分叉发生在“返回值处理”阶段。

## 10. REST 接口为什么没有 `ModelAndView`？

响应体处理器会执行：

```java
mavContainer.setRequestHandled(true);
```

随后：

```java
RequestMappingHandlerAdapter#getModelAndView()
    if (mavContainer.isRequestHandled()) {
        return null;
    }
```

因此 `DispatcherServlet` 不会执行 `ViewResolver`。

[[04-Controller参数解析|上一篇：Controller 参数解析]] · [[SpringMVC-HTTP请求处理全链路|返回索引]] · [[06-异常处理与拦截器|下一篇：异常处理与拦截器]]
