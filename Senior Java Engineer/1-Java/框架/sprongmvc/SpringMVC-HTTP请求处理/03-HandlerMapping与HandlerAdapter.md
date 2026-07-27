---
title: Spring MVC HandlerMapping 与 HandlerAdapter
tags:
  - Java
  - SpringMVC
  - HandlerMapping
  - HandlerAdapter
  - 源码分析
---

# 03 HandlerMapping 与 HandlerAdapter

[[02-Servlet入口与DispatcherServlet|上一篇：Servlet 入口与 DispatcherServlet]] · [[SpringMVC-HTTP请求处理全链路|返回索引]] · [[04-Controller参数解析|下一篇：Controller 参数解析]]

## 1. 两个组件的职责

```text
HandlerMapping
  回答：谁来处理请求？
  输出：HandlerExecutionChain

HandlerAdapter
  回答：怎样调用这个Handler？
  输出：ModelAndView或直接写响应
```

两者分离之后：

- 路由匹配可以独立扩展；
- Handler 的调用方式可以独立扩展；
- Interceptor 可以插入查找和调用之间；
- `DispatcherServlet` 不需要了解 Controller 的反射细节。

## 2. 映射在启动阶段已经建立

请求到来之前：

```text
RequestMappingHandlerMapping.afterPropertiesSet()
  -> initHandlerMethods()
  -> detectHandlerMethods()
  -> 读取@Controller和@RequestMapping
  -> registerHandlerMethod()
  -> MappingRegistry
```

注册信息包含：

```text
RequestMappingInfo
  ├─ path
  ├─ HTTP method
  ├─ params
  ├─ headers
  ├─ consumes
  ├─ produces
  └─ custom conditions

HandlerMethod
  ├─ Controller Bean
  ├─ Java Method
  ├─ MethodParameter[]
  └─ annotations
```

> [!important]
> 每次请求不会重新扫描所有 Controller。请求阶段主要是查映射表并匹配条件。

## 3. `getHandler()` 调用链

示例请求：

```text
POST /api/users/42/orders?notify=true
Content-Type: application/json
Accept: application/json
```

调用链：

```java
DispatcherServlet#getHandler(request)                                // ★重要
    // 遍历所有HandlerMapping

    AbstractHandlerMapping#getHandler(request)

        AbstractHandlerMethodMapping#getHandlerInternal(request)

            AbstractHandlerMethodMapping#lookupHandlerMethod(
                    "/api/users/42/orders", request)                  // ★核心

                MappingRegistry#getMappingsByDirectPath()
                    // 获取路径直接匹配的候选项

                RequestMappingInfoHandlerMapping#getMatchingMapping()

                    RequestMappingInfo#getMatchingCondition(request)

                        RequestMethodsRequestCondition
                            #getMatchingCondition()
                            // POST是否匹配

                        ParamsRequestCondition
                            #getMatchingCondition()
                            // params条件是否匹配

                        HeadersRequestCondition
                            #getMatchingCondition()
                            // headers条件是否匹配

                        ConsumesRequestCondition
                            #getMatchingCondition()
                            // Content-Type是否匹配consumes

                        ProducesRequestCondition
                            #getMatchingCondition()
                            // Accept是否匹配produces

                        PathPatternsRequestCondition
                            #getMatchingCondition()
                            // 路径是否匹配
                            // 提取userId=42

                // 如果有多个候选项，比较优先级
                // 最终得到最具体的HandlerMethod

                return HandlerMethod(
                        bean   = orderController,
                        method = createOrder(...)
                );
```

## 4. 匹配失败的情况

| 失败位置 | 示例结果 |
| --- | --- |
| path 不匹配 | 找不到 Handler，通常 404 |
| HTTP method 不匹配 | 可能 405 |
| consumes 不匹配 | 415 |
| produces 无法满足 Accept | 406 |
| 多个映射同样具体 | Ambiguous handler 异常 |

`Content-Type` 与 `Accept`：

```text
Content-Type
  描述客户端发送的请求体格式
  参与consumes匹配

Accept
  描述客户端希望接收的响应格式
  参与produces匹配
```

## 5. `HandlerExecutionChain`

找到 `HandlerMethod` 后：

```java
AbstractHandlerMapping#getHandlerExecutionChain()
    // 添加与当前路径匹配的HandlerInterceptor

    return new HandlerExecutionChain(
            handlerMethod,
            interceptor1,
            interceptor2
    );
```

结构：

```text
HandlerExecutionChain
  ├─ HandlerMethod
  │    ├─ Controller Bean
  │    └─ Java Method
  └─ HandlerInterceptor[]
```

`HandlerMapping` 到这里结束，它不会直接调用 Controller。

## 6. 选择 `HandlerAdapter`

```java
DispatcherServlet#getHandlerAdapter(handlerMethod)
    // 按顺序遍历所有HandlerAdapter

    RequestMappingHandlerAdapter#supports(handlerMethod)
        // Handler是HandlerMethod，匹配成功

    return RequestMappingHandlerAdapter;
```

适配器模式让 `DispatcherServlet` 可以支持不同 Handler：

```text
HandlerAdapter
  ├─ RequestMappingHandlerAdapter
  ├─ HttpRequestHandlerAdapter
  └─ SimpleControllerHandlerAdapter
```

## 7. 执行 `preHandle()`

```java
HandlerExecutionChain#applyPreHandle(request, response)

    interceptor1#preHandle(request, response, handlerMethod)

    interceptor2#preHandle(request, response, handlerMethod)

    // 全部返回true才继续调用Controller
```

如果某个拦截器返回 `false`：

- 后续拦截器不再执行；
- Controller 不执行；
- 已执行成功的拦截器逆序执行 `afterCompletion()`；
- 拦截器应自行完成必要的响应。

## 8. 准备调用 Controller

```java
AbstractHandlerMethodAdapter#handle(
        request, response, handlerMethod)

    RequestMappingHandlerAdapter#handleInternal(...)

        RequestMappingHandlerAdapter#invokeHandlerMethod(...)         // ★核心

            new ServletWebRequest(request, response)

            getDataBinderFactory()
                // 数据绑定、类型转换、校验

            getModelFactory()
                // @ModelAttribute、@SessionAttributes

            new ModelAndViewContainer()

            ModelFactory#initModel()

            new ServletInvocableHandlerMethod(handlerMethod)
                // 设置ArgumentResolver
                // 设置ReturnValueHandler

            ServletInvocableHandlerMethod#invokeAndHandle()
```

从这里开始进入参数解析和 Controller 反射调用。

[[02-Servlet入口与DispatcherServlet|上一篇：Servlet 入口与 DispatcherServlet]] · [[SpringMVC-HTTP请求处理全链路|返回索引]] · [[04-Controller参数解析|下一篇：Controller 参数解析]]
