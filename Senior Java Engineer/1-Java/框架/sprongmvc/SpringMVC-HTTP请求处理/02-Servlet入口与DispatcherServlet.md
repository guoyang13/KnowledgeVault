---
title: Spring MVC Servlet 入口与 DispatcherServlet
tags:
  - Java
  - SpringMVC
  - DispatcherServlet
  - 源码分析
---

# 02 Servlet 入口与 DispatcherServlet

[[01-请求处理总览|上一篇：请求处理总览]] · [[SpringMVC-HTTP请求处理全链路|返回索引]] · [[03-HandlerMapping与HandlerAdapter|下一篇：HandlerMapping 与 HandlerAdapter]]

## 1. Tomcat 与 Filter 在 Spring MVC 之前

Tomcat 部分的具体类随版本变化，主流程如下：

```java
客户端发送HTTP字节
    Tomcat#Http11InputBuffer#parseRequestLine()
        // 解析：POST /api/users/42/orders?notify=true HTTP/1.1

    Tomcat#Http11InputBuffer#parseHeaders()
        // 解析Content-Type、Accept、X-Request-Id等请求头

    Tomcat#CoyoteAdapter#service()
        // 适配为ServletRequest和ServletResponse

    Tomcat#StandardEngineValve#invoke()
        Tomcat#StandardHostValve#invoke()
            Tomcat#StandardContextValve#invoke()
                Tomcat#StandardWrapperValve#invoke()
                    // 根据Servlet映射找到DispatcherServlet

                    ApplicationFilterChain#doFilter()
                        CharacterEncodingFilter#doFilter()
                            Security Filter链
                                业务Filter#doFilter()
                                    HttpServlet#service(
                                            ServletRequest,
                                            ServletResponse)
```

> [!important]
> `FrameworkServlet.doPost()` 不是请求的第一个入口。在它之前已经经过 Tomcat、Servlet 映射和 Filter 链。

## 2. 谁调用 `FrameworkServlet.doPost()`？

```java
HttpServlet#service(ServletRequest, ServletResponse)
    // 校验并转换为HttpServletRequest、HttpServletResponse

    FrameworkServlet#service(
            HttpServletRequest request,
            HttpServletResponse response)
        // POST是标准HTTP方法，因此调用父类service

        HttpServlet#service(
                HttpServletRequest request,
                HttpServletResponse response)
            // 根据request.getMethod()分派

            if ("POST".equals(request.getMethod())) {
                doPost(request, response);
            }

            FrameworkServlet#doPost(request, response)
                // Java多态：执行FrameworkServlet重写的doPost

                FrameworkServlet#processRequest(request, response)
```

直接调用 `doPost()` 的是：

```text
HttpServlet.service(HttpServletRequest, HttpServletResponse)
```

`super.service()` 虽然进入父类执行，但父类内部的 `doPost()` 调用具有多态性，因此最终回到 `FrameworkServlet.doPost()`。

## 3. 不同 HTTP 方法的入口

| HTTP 方法 | FrameworkServlet 入口 |
| --- | --- |
| GET | `doGet()` |
| POST | `doPost()` |
| PUT | `doPut()` |
| DELETE | `doDelete()` |
| PATCH、非标准方法 | `service()` 直接调用 `processRequest()` |
| OPTIONS | `doOptions()` |
| TRACE | `doTrace()` |

所以：

```text
每个进入DispatcherServlet的POST请求
  通常经过FrameworkServlet.doPost()

每个HTTP请求
  不一定经过FrameworkServlet.doPost()
```

### `doPost()` 和 `@PostMapping`

```text
FrameworkServlet.doPost()
  Servlet层确认这是POST请求

@PostMapping
  Spring MVC层确定由哪个Controller方法处理
```

顺序是：

```text
POST
  -> doPost()
  -> DispatcherServlet.doDispatch()
  -> HandlerMapping
  -> 匹配@PostMapping
```

## 4. `FrameworkServlet.processRequest()`

`doGet()`、`doPost()`、`doPut()`、`doDelete()` 最终都会汇合到：

```java
FrameworkServlet#processRequest(request, response)                    // ★重要
    // 记录请求开始时间
    // 创建并绑定LocaleContext
    // 创建并绑定RequestAttributes
    // 注册异步请求拦截器

    doService(request, response)
        // 模板方法，实际执行DispatcherServlet.doService()

    finally
        // 恢复线程原有上下文
        // 发布ServletRequestHandledEvent
```

它解决的是 Spring Web 层的公共上下文管理，而不是业务路由。

## 5. `DispatcherServlet.doService()`

```java
DispatcherServlet#doService(request, response)
    // 将以下对象暴露到request属性：
    // WebApplicationContext
    // LocaleResolver
    // FlashMapManager
    // InputFlashMap
    // OutputFlashMap

    DispatcherServlet#doDispatch(request, response)
```

## 6. `DispatcherServlet.doDispatch()`

这是 Spring MVC 请求处理的核心总控：

```java
DispatcherServlet#doDispatch(request, response)                      // ★核心

    processedRequest = checkMultipart(request);

    mappedHandler = getHandler(processedRequest);
        // HandlerMethod + Interceptors

    handlerAdapter =
            getHandlerAdapter(mappedHandler.getHandler());

    if (!mappedHandler.applyPreHandle(request, response)) {
        return;
    }

    ModelAndView mv = handlerAdapter.handle(
            request, response, mappedHandler.getHandler());

    mappedHandler.applyPostHandle(request, response, mv);

    processDispatchResult(
            request, response,
            mappedHandler, mv, dispatchException);
```

真实源码还负责：

- Last-Modified；
- multipart 包装与资源清理；
- 默认视图名；
- 404；
- 同步与异步请求；
- `Exception` 和 `Error`；
- `afterCompletion()`。

## 7. `checkMultipart()`

```text
普通JSON或表单请求
  -> 继续使用原HttpServletRequest

multipart/form-data
  -> MultipartResolver
  -> MultipartHttpServletRequest
```

文件上传请求可能在这里被包装，以便后续参数解析器获取文件。

## 8. `doDispatch()` 的输入与输出

```text
输入：
  HttpServletRequest
  HttpServletResponse

中间状态：
  HandlerExecutionChain
  HandlerAdapter
  ModelAndView
  dispatchException

输出：
  直接写入HttpServletResponse
  或渲染View
  或抛出未处理异常
```

下一篇继续分析 `getHandler()` 和 `getHandlerAdapter()`。

[[01-请求处理总览|上一篇：请求处理总览]] · [[SpringMVC-HTTP请求处理全链路|返回索引]] · [[03-HandlerMapping与HandlerAdapter|下一篇：HandlerMapping 与 HandlerAdapter]]
