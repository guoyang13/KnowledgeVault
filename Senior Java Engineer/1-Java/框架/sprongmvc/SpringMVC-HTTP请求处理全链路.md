---
title: Spring MVC HTTP 请求处理全链路
aliases:
  - SpringMVC 请求处理流程
  - Spring MVC 源码调用链
tags:
  - Java
  - Spring
  - SpringMVC
  - MOC
---

# Spring MVC HTTP 请求处理全链路

> [!abstract] 一句话结论
> Servlet 容器把 HTTP 字节解析为 `HttpServletRequest`，`DispatcherServlet` 再负责总调度：`HandlerMapping` 找 Controller，`HandlerAdapter` 调 Controller，参数解析器把 HTTP 数据变成 Java 参数，返回值处理器把 Java 返回值变成 HTTP 响应。

这是一篇导航笔记。完整内容已按职责拆分，建议按编号阅读。

## 学习目录

| 顺序 | 笔记 | 解决的问题 |
| --- | --- | --- |
| 01 | [[SpringMVC-HTTP请求处理/01-请求处理总览\|请求处理总览]] | 一次请求整体经过哪些阶段？ |
| 02 | [[SpringMVC-HTTP请求处理/02-Servlet入口与DispatcherServlet\|Servlet 入口与 DispatcherServlet]] | `doPost()` 谁调用？`doDispatch()` 做什么？ |
| 03 | [[SpringMVC-HTTP请求处理/03-HandlerMapping与HandlerAdapter\|HandlerMapping 与 HandlerAdapter]] | 怎样找到并准备调用 Controller？ |
| 04 | [[SpringMVC-HTTP请求处理/04-Controller参数解析\|Controller 参数解析]] | URL、请求头和 JSON 怎样变成 Java 参数？ |
| 05 | [[SpringMVC-HTTP请求处理/05-Controller返回值处理\|Controller 返回值处理]] | `ResponseEntity`、`@ResponseBody`、视图名怎样生成响应？ |
| 06 | [[SpringMVC-HTTP请求处理/06-异常处理与拦截器\|异常处理与拦截器]] | 异常、Filter 和 Interceptor 在哪里执行？ |
| 07 | [[SpringMVC-HTTP请求处理/07-源码调试实战\|源码调试实战]] | 在当前 Spring Framework 项目中怎样断点学习？ |

## 核心接口地图

> [!abstract] 阅读方法
> 按“容器入口、处理器定位、方法调用、参数转换、返回值转换、异常与横切”定位接口。每次追源码先判断当前阶段是在找 Controller、调用 Controller，还是把 HTTP 与 Java 对象互相转换。

### Servlet 容器入口

| API / 类型 | 处理对象 | 时机 | 核心职责 |
| --- | --- | --- | --- |
| `Servlet` / `HttpServlet` | Servlet 请求与响应 | FilterChain 末端 | 定义 Web 组件处理请求的标准协议 |
| `Filter` | `ServletRequest` / `ServletResponse` | Servlet 前后 | 执行编码、安全、日志等容器级横切逻辑 |
| `FilterChain` | Filter 与目标 Servlet | 请求进入时 | 按注册顺序推进 Filter，最终调用 `DispatcherServlet` |
| `ServletRequest` / `HttpServletRequest` | 已解析的 HTTP 请求 | Tomcat 解析字节后 | 暴露方法、URI、Header、参数、Body 输入流等信息 |
| `ServletResponse` / `HttpServletResponse` | HTTP 响应 | 整个请求周期 | 暴露状态码、Header 和响应体输出流 |

主要实现：`DispatcherServlet` 继承 `FrameworkServlet` 和 `HttpServlet`，是 Servlet 规范与 Spring MVC 调度体系的连接点。

### 查找与调用 Controller

| API / 类型 | 中文定位 | 输入 | 输出 / 作用 |
| --- | --- | --- | --- |
| `HandlerMapping` | 处理器路由器 | 当前请求 | 返回处理器及拦截器组成的 `HandlerExecutionChain` |
| `HandlerExecutionChain` | 调用链描述 | Handler + Interceptors | 保存本次请求要调用的处理器和拦截器链 |
| `HandlerAdapter` | 处理器调用适配器 | Handler 对象 | 用适合该 Handler 的方式发起调用 |
| `HandlerMethod` | Controller 方法描述 | Bean、Method、参数元数据 | 表达最终要执行的 Controller 方法 |
| `HandlerInterceptor` | MVC 调用拦截器 | Handler 调用前后 | 执行 `preHandle`、`postHandle`、`afterCompletion` |

常见实现：`RequestMappingHandlerMapping` 负责找到 `@RequestMapping` 方法，`RequestMappingHandlerAdapter` 负责参数解析、反射调用和返回值处理。

### Controller 参数解析

| API / 类型 | 处理对象 | 典型实现 | 核心职责 |
| --- | --- | --- | --- |
| `HandlerMethodArgumentResolver` | 单个方法参数 | `RequestParamMethodArgumentResolver`、`PathVariableMethodArgumentResolver` | 判断是否支持并生成参数值 |
| `WebDataBinderFactory` | Binder 创建请求 | `ServletRequestDataBinderFactory` | 为当前方法创建 `WebDataBinder` |
| `WebDataBinder` | 请求字段与 Java 对象 | `ServletRequestDataBinder` | 数据绑定、类型转换、校验并保存错误信息 |
| `ConversionService` | Java 类型转换 | `FormattingConversionService` | 完成 String、数字、日期、枚举等转换 |
| `Validator` | 绑定后的对象 | Bean Validation 适配器 | 执行 `@Valid`、`@Validated` 校验 |
| `HttpMessageConverter<T>` | HTTP Body 与 Java 对象 | `MappingJackson2HttpMessageConverter` | 根据 Content-Type 读取 JSON 等请求体 |

### 返回值、视图与异常

| API / 类型 | 处理对象 | 典型实现 | 核心职责 |
| --- | --- | --- | --- |
| `HandlerMethodReturnValueHandler` | Controller 返回值 | `RequestResponseBodyMethodProcessor`、`HttpEntityMethodProcessor` | 判断返回值语义并写响应或生成 ModelAndView |
| `HttpMessageConverter<T>` | Java 对象与 HTTP Body | `MappingJackson2HttpMessageConverter` | 根据协商结果序列化 JSON 等响应体 |
| `ModelAndView` | 模型和逻辑视图名 | — | 承载视图渲染所需信息 |
| `ViewResolver` | 逻辑视图名 | `InternalResourceViewResolver`、模板引擎 Resolver | 定位可执行的 `View` |
| `View` | Model 与响应 | JSP、Thymeleaf 等 View | 将模型渲染到 `HttpServletResponse` |
| `HandlerExceptionResolver` | 调用链异常 | `ExceptionHandlerExceptionResolver` 等 | 把异常转换成 ModelAndView 或 HTTP 响应 |

异常解析器通常按以下顺序参与：`@ExceptionHandler` → `@ResponseStatus` / `ResponseStatusException` → Spring MVC 默认异常映射。

### 三组边界接口

| 边界 | 左侧职责 | 右侧职责 |
| --- | --- | --- |
| `Filter` → `DispatcherServlet` | Servlet 容器级横切 | Spring MVC 请求调度 |
| `HandlerMapping` → `HandlerAdapter` | 找到“调用谁” | 决定“怎样调用” |
| `ArgumentResolver` → `Controller` → `ReturnValueHandler` | HTTP 转 Java 参数 | 执行业务，再把 Java 返回值转回 HTTP 语义 |

## 形象类比：餐厅接单与出餐

> [!warning] 使用边界
> 类比只帮助记忆职责分工。判断 Filter、Interceptor、Resolver 的真实时序时，仍应回到 `doFilter()`、`doDispatch()` 和 HandlerAdapter 源码。

| 餐厅接单类比 | Spring MVC 类型 | 真正职责 |
| --- | --- | --- |
| 顾客递交原始订单 | HTTP 请求字节 | 携带方法、路径、Header 和 Body |
| 门口接待把口述内容登记成订单 | Tomcat / `HttpServletRequest` | 解析 HTTP 字节并生成 Servlet 请求对象 |
| 门禁、安检、统一登记 | `Filter` / `FilterChain` | 在进入 MVC 前执行安全、编码、日志等处理 |
| 前厅总调度经理 | `DispatcherServlet` | 统筹本次请求从路由到响应的完整流程 |
| 查排班表，确定由哪位厨师处理 | `HandlerMapping` | 根据请求找到 Controller 方法 |
| 厨房联络员 | `HandlerAdapter` | 理解处理器类型并组织具体调用 |
| 订单上记录的厨师和菜品方法 | `HandlerMethod` | 描述 Controller Bean 与目标 Method |
| 配菜员把订单要求准备成标准食材 | `HandlerMethodArgumentResolver` | 把路径、参数、Header、Body 解析成 Java 参数 |
| 翻译外语订单和出餐说明 | `HttpMessageConverter` | 在 HTTP Body 与 Java 对象之间转换 |
| 真正烹饪 | Controller 方法 | 执行业务逻辑并产生 Java 返回值 |
| 出餐协调员 | `HandlerMethodReturnValueHandler` | 判断返回 JSON、状态码、视图还是其他响应形式 |
| 根据桌号找到摆盘模板 | `ViewResolver` / `View` | 定位并渲染服务器端视图 |
| 客诉与应急处理台 | `HandlerExceptionResolver` | 把异常转换为受控响应 |
| 厨房流程检查员 | `HandlerInterceptor` | 在 Controller 调用前后执行 MVC 级横切逻辑 |
| 门口完成离店登记 | Filter 逆序返回 | 在整个 Servlet 调用结束后执行收尾逻辑 |

完整类比流程：

```text
顾客提交订单
  -> 门口登记成标准订单
  -> 通过门禁和安检
  -> 前厅经理接单
  -> 查排班表找到厨师
  -> 联络员组织调用
  -> 配菜员准备标准参数
  -> 厨师完成菜品
  -> 出餐协调员决定 JSON、状态码或视图
  -> 异常时交给应急处理台
  -> 检查员和门禁逆序收尾
```

对应正式链路：

```text
Tomcat
  -> FilterChain
  -> DispatcherServlet
  -> HandlerMapping
  -> HandlerAdapter
  -> HandlerMethodArgumentResolver
  -> Controller
  -> HandlerMethodReturnValueHandler
  -> HttpMessageConverter / ViewResolver
  -> HandlerExceptionResolver（异常分支）
  -> Interceptor + Filter 收尾
```

## 推荐阅读方式

### 第一次：只建立主线

阅读：

```text
01 请求处理总览
  -> 02 Servlet入口与DispatcherServlet
  -> 03 HandlerMapping与HandlerAdapter
```

目标是记住：

```text
DispatcherServlet
  -> HandlerMapping
  -> HandlerAdapter
  -> Controller
```

### 第二次：理解数据转换

阅读：

```text
04 Controller参数解析
  -> 05 Controller返回值处理
```

目标是理解：

```text
HTTP请求
  -> Java参数
  -> Controller返回值
  -> HTTP响应
```

### 第三次：带着调试深入源码

阅读：

```text
06 异常处理与拦截器
  -> 07 源码调试实战
```

然后运行 `redirectAttribute` 测试，按推荐断点单步跟踪。

## 最终记忆链

```text
Tomcat解析HTTP
  -> FilterChain
  -> HttpServlet.service()
  -> FrameworkServlet.doPost()
  -> FrameworkServlet.processRequest()
  -> DispatcherServlet.doService()
  -> DispatcherServlet.doDispatch()
  -> HandlerMapping找Controller
  -> HandlerAdapter准备调用
  -> ArgumentResolver解析参数
  -> HttpMessageConverter读取请求体
  -> Method.invoke()调用Controller
  -> ReturnValueHandler处理返回值
  -> HttpMessageConverter写响应体
     或ViewResolver渲染视图
  -> ExceptionResolver处理异常
  -> Interceptor和Filter收尾
  -> Tomcat返回HTTP响应
```

## 三个最重要的方法

| 方法 | 记忆点 |
| --- | --- |
| `DispatcherServlet.doDispatch()` | 一次 Spring MVC 请求的总流程 |
| `InvocableHandlerMethod.getMethodArgumentValues()` | HTTP 请求怎样变成 Java 参数 |
| `HandlerMethodReturnValueHandlerComposite.handleReturnValue()` | Java 返回值怎样变成 HTTP 响应 |

## 三个最重要的分界线

```text
HandlerMapping -> HandlerAdapter
  从“找到谁”进入“怎样调用”

getMethodArgumentValues() -> doInvoke()
  从“准备参数”进入“执行业务”

Controller返回 -> ReturnValueHandler
  从“Java返回值”进入“HTTP响应”
```

> [!tip]
> 不要尝试一次记住全部实现类。先记住主链路，再根据遇到的注解追踪对应 Resolver 或 ReturnValueHandler。
