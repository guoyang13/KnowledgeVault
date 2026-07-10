# JDK 8 到 JDK 23：JVM 重要变化

> 版本演进总览。专题深挖见 [[1000-JVM专题导航]]（1001～1006）。

## 一句话概括

从 JDK 8 到 JDK 23，JVM 的核心变化不是“多了几个语法糖”，而是从传统服务端运行时，演进成了：

- 更模块化
- 更强封装
- 更低延迟 GC
- 更强可观测性
- 更适合云原生和容器
- 支持海量轻量线程
- 更适合现代框架和运行时代码生成

## 1. 内存实现变化

JVM 规范里的运行时数据区变化不大，仍然可以理解为：

```text
线程私有：
- 程序计数器
- Java 虚拟机栈
- 本地方法栈

线程共享：
- Java 堆
- 方法区
- 运行时常量池
```

真正变化大的是 HotSpot 的实现。

### JDK 8：永久代移除

JDK 8 移除了 [[永久代]]，改为 [[Metaspace]]。

```text
JDK 7 及以前：
方法区的 HotSpot 实现 ≈ PermGen

JDK 8 以后：
方法区的 HotSpot 实现 ≈ Metaspace
```

重要区别：

- PermGen 使用 JVM 管理的内存。
- Metaspace 使用本地内存。
- 类元数据进入 Metaspace。
- 字符串常量和类静态变量关联对象更多进入 Java Heap。

常见参数变化：

```bash
# JDK 7 及以前
-XX:PermSize
-XX:MaxPermSize

# JDK 8 以后
-XX:MetaspaceSize
-XX:MaxMetaspaceSize
```

### JDK 9：String 内存结构变化

JDK 9 引入 Compact Strings。

```text
JDK 8:
String 内部主要是 char[]

JDK 9+:
String 内部主要是 byte[] + coder
```

如果字符串只包含 Latin-1 字符，内存占用可以更低。

### JDK 21：虚拟线程影响栈的实际承载方式

平台线程通常对应 OS Thread，线程栈主要是 native stack。

虚拟线程数量可以非常多，它的栈帧可以以 stack chunk 的形式保存在 heap 中。

所以虚拟线程不是改变了 JVM 规范里的“虚拟机栈”概念，而是改变了 HotSpot 对线程执行状态的实际管理方式。

## 2. GC 体系变化

JDK 8 到 JDK 23 的 GC 变化非常重要。

### JDK 8 常见情况

JDK 8 常见默认是 Parallel GC，很多服务端项目也会使用 CMS 或 G1。

```text
常见 GC：
- Serial
- Parallel
- CMS
- G1
```

### JDK 9：G1 成为默认 GC

JDK 9 开始，G1 成为默认 GC。详见 [[1004-G1垃圾回收与堆内存]]。

G1 的堆不再只是物理上连续地分为 Young / Old，而是切成多个 Region：

```text
Heap
├── Eden Region
├── Survivor Region
├── Old Region
└── Humongous Region
```

逻辑上仍然有分代思想，但物理管理方式更加 Region 化。

### JDK 11 到 JDK 17：低延迟 GC 成熟

这一阶段重点是低延迟 GC。ZGC 详见 [[1005-ZGC垃圾回收与堆内存]]。

- JDK 11：ZGC 实验性引入。
- JDK 12：Shenandoah 实验性引入。
- JDK 14：CMS 被移除。
- JDK 15：ZGC 和 Shenandoah 转正。
- JDK 17：长期支持版本，G1、ZGC、Shenandoah 更成熟。

### JDK 23：ZGC 默认分代模式

JDK 23 中，如果使用：

```bash
-XX:+UseZGC
```

默认使用 Generational ZGC。详见 [[1005-ZGC垃圾回收与堆内存]] §13。

## 3. 模块化与强封装

JDK 9 引入 [[JPMS]]，也就是 Java Platform Module System。

它带来的变化：

- JDK 自身被拆成多个模块。
- 除了 classpath，还有 module-path。
- 支持 module-info.java。
- 可以用 jlink 构建定制运行时镜像。
- JDK 内部 API 访问逐步被限制。

### 对老项目的影响

JDK 8 升级到 JDK 11、17、21 时，经常遇到：

```text
Illegal reflective access
InaccessibleObjectException
```

原因通常是：

- 老框架反射访问 JDK 内部类。
- 使用 sun.*、com.sun.*、jdk.* 内部 API。
- 字节码增强工具版本过旧。
- Lombok、CGLIB、ASM、ByteBuddy 版本不兼容高版本 JDK。

常见临时解决方式：

```bash
--add-opens java.base/java.lang=ALL-UNNAMED
--add-opens java.base/java.util=ALL-UNNAMED
--add-exports java.base/sun.nio.ch=ALL-UNNAMED
```

但长期看，应该升级依赖，而不是长期依赖 `--add-opens`。

## 4. JVM 日志与可观测性变化

### JDK 9：统一 JVM 日志

JDK 9 引入统一日志系统：

```bash
-Xlog
```

以前常见的 GC 日志参数：

```bash
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-Xloggc:gc.log
```

在新版本中更推荐：

```bash
-Xlog:gc*:file=gc.log:time,uptime,level,tags
```

还可以看类加载、Safepoint、JIT 等（[[1001-ClassLoader类加载机制|类加载]]、[[1002-SPI机制|SPI]]、[[1003-TCCL线程上下文类加载器|TCCL]]）：

```bash
-Xlog:class+load
-Xlog:safepoint
-Xlog:compiler
```

### JDK 11：JFR 开源并内置

JDK 11 中，[[JFR]] 成为 OpenJDK 的一部分。

JFR 可以低开销记录：

- GC
- 线程
- 锁竞争
- 对象分配
- 方法采样
- IO
- 异常
- 类加载
- JVM 内部事件

常用命令：

```bash
jcmd <pid> JFR.start
jcmd <pid> JFR.dump filename=recording.jfr
jcmd <pid> JFR.stop
```

线上性能问题排查时，JFR 比传统日志更适合做事后分析。

## 5. 字节码与运行时代码生成增强

### JDK 8：Lambda 与 invokedynamic

JDK 8 的 Lambda 不是简单匿名内部类，而是依赖：

```text
invokedynamic
LambdaMetafactory
```

这让 JVM 可以在运行时更灵活地链接方法调用。

### JDK 11：Nest-Based Access Control

JDK 11 引入 nestmate 机制。

作用是：让逻辑上属于同一个嵌套类结构的多个 class 文件，可以更自然地访问彼此的 private 成员，减少编译器生成桥接方法。

### JDK 11：Dynamic Class-File Constants

JDK 11 引入 `CONSTANT_Dynamic`，可以把常量的创建推迟到链接阶段，通过 bootstrap method 解析。

它对语言实现、动态语言、运行时代码生成、MethodHandle 生态都有意义。

### JDK 15：Hidden Classes

Hidden Classes 主要服务于：

- Lambda
- 动态代理
- 字节码框架
- 运行时代码生成

它适合生成不需要被普通应用代码直接引用的类。

## 6. 并发模型变化

### JDK 8 到 JDK 17

这期间并发 API 逐步增强，但主流服务端仍然依赖线程池模型：

```text
请求 -> 线程池 -> 平台线程 -> 阻塞 IO
```

线程是相对昂贵的资源，所以需要控制线程池大小。

### JDK 21：虚拟线程

JDK 21 引入虚拟线程。

传统模型：

```text
Java Thread ≈ OS Thread
```

虚拟线程模型：

```text
Virtual Thread 很轻量
阻塞时可以释放底层平台线程
适合一个请求一个虚拟线程
```

虚拟线程适合：

- IO 密集型服务
- 同步阻塞风格代码
- 高并发请求处理

不适合期待它直接提升 CPU 密集型计算性能。

理解重点：

```text
虚拟线程不是让 CPU 更快，而是让阻塞型并发更便宜。
```

## 7. 安全模型变化

### Security Manager 退场

JDK 17 开始，Security Manager 被标记为将来移除。

原因是它历史包袱重，现代服务端很少再依赖它做主要安全边界。

### Finalization 退场

JDK 18 开始，finalization 被标记为将来移除。

不推荐再依赖：

```java
protected void finalize() {
    // 清理资源
}
```

资源清理应该使用：

- try-with-resources
- AutoCloseable
- Cleaner
- 显式 close

## 8. Native 与外部内存交互变化

JDK 22 中 Foreign Function & Memory API 转正。

它属于 Project Panama，目标是提供比 JNI 更现代的方式访问：

- native 函数
- native memory
- C library
- off-heap 数据

适合场景：

- 高性能中间件
- 数据库
- 向量计算
- 底层系统库调用
- 替代部分 JNI 场景

## 9. 容器与云原生适配

JDK 8 早期对容器支持不完善，容易出现 JVM 看到宿主机资源，而不是容器资源的问题。

高版本 JDK 对容器支持更好：

- 能识别 cgroup 限制。
- 堆大小计算更适合容器。
- CPU 数量识别更准确。
- 更适合 Kubernetes 部署。

常见参数：

```bash
-XX:MaxRAMPercentage=75
-XX:InitialRAMPercentage=50
-XX:ActiveProcessorCount=2
```

容器环境下不要只看 `-Xmx`，还要关注：

- Metaspace
- Direct Memory
- Thread Stack
- Code Cache
- Native Memory
- GC 内部内存

## 10. 语言特性对 JVM 生态的影响

这些特性本身不是全部属于 JVM，但会影响编译器、字节码和框架设计。

```text
JDK 8:
- Lambda
- Stream
- Optional
- CompletableFuture

JDK 10:
- var

JDK 14:
- switch 表达式
- Helpful NullPointerException

JDK 16:
- record 转正

JDK 17:
- sealed class 转正

JDK 21:
- virtual threads
- record pattern
- pattern matching for switch

JDK 22/23:
- Foreign Function & Memory API
- Class-File API
- 模式匹配继续演进
```

## 11. 从 JDK 8 升级到 JDK 17/21/23 的常见坑

### 反射与内部 API

常见报错：

```text
java.lang.reflect.InaccessibleObjectException
IllegalAccessError
Illegal reflective access
```

处理思路：

1. 升级框架和依赖。
2. 升级 Lombok、ASM、ByteBuddy、CGLIB。
3. 临时加 `--add-opens`。
4. 长期移除对 JDK 内部 API 的依赖。

### GC 参数变化

CMS 已移除，老参数会失效：

```bash
-XX:+UseConcMarkSweepGC
```

GC 日志参数也建议换成：

```bash
-Xlog:gc*
```

### Java EE / CORBA 模块移除

JDK 11 移除了一些 Java EE 和 CORBA 模块。

常见缺失：

```text
javax.xml.bind
javax.activation
javax.annotation
CORBA
```

处理方式：显式引入 Maven 依赖。

### Nashorn 移除

Nashorn JavaScript 引擎在高版本 JDK 中被移除。

如果项目依赖：

```java
ScriptEngineManager
```

需要检查是否使用了 Nashorn。

### 默认字符集变化

JDK 18 起默认字符集统一为 UTF-8。

这通常是好事，但老项目如果依赖系统默认编码，可能出现兼容问题。

建议显式指定编码：

```java
StandardCharsets.UTF_8
```

## 12. 面试回答模板

如果被问：

> JDK 8 到 JDK 23，JVM 除了内存分区还有哪些重要变化？

可以这样答：

```text
我会从几条主线看：

第一，模块化。JDK 9 引入 JPMS，JDK 内部 API 被逐步强封装，所以高版本升级经常遇到反射访问失败。

第二，GC。JDK 9 默认 G1，CMS 后来被移除，ZGC 和 Shenandoah 成熟，JDK 23 使用 ZGC 时默认走分代 ZGC。

第三，可观测性。JDK 9 引入统一日志 -Xlog，JDK 11 内置 JFR，线上诊断能力明显增强。

第四，字节码和运行时代码生成。JDK 8 的 Lambda 依赖 invokedynamic，JDK 11 有 nestmate 和 dynamic constants，JDK 15 有 hidden classes。

第五，并发模型。JDK 21 引入虚拟线程，让同步阻塞式服务在高并发 IO 场景下更容易扩展。

第六，安全和兼容性。Security Manager、finalize 等老机制逐步退出，JDK 内部 API 和老 GC 参数都需要迁移。
```

## 13. 学习顺序

建议从 [[1000-JVM专题导航]] 进入，按模块学习：

**模块二：GC 与内存（建议先学）**

```text
1006 内存分配  →  1004 G1  →  1005 ZGC
```

**模块一：类加载**

```text
1001 ClassLoader  →  1002 SPI  →  1003 TCCL
```

**版本演进（本篇）**

```text
1. 本篇概览：Metaspace、模块化、JFR、虚拟线程
2. 理解 JDK 8 永久代 → Metaspace
3. JDK 9 模块化与强封装
4. JDK 11 JFR、Nest-Based Access
5. JDK 17 LTS 升级兼容性
6. JDK 21 虚拟线程
7. JDK 23+ 默认分代 ZGC 等趋势
```

## 14. 参考资料

- [JEP 122: Remove the Permanent Generation](https://openjdk.org/jeps/122)
- [JEP 248: Make G1 the Default Garbage Collector](https://openjdk.org/jeps/248)
- [JEP 261: Module System](https://openjdk.org/jeps/261)
- [JEP 158: Unified JVM Logging](https://openjdk.org/jeps/158)
- [JEP 328: Flight Recorder](https://openjdk.org/jeps/328)
- [JEP 181: Nest-Based Access Control](https://openjdk.org/jeps/181)
- [JEP 309: Dynamic Class-File Constants](https://openjdk.org/jeps/309)
- [JEP 371: Hidden Classes](https://openjdk.org/jeps/371)
- [JEP 403: Strongly Encapsulate JDK Internals](https://openjdk.org/jeps/403)
- [JEP 411: Deprecate the Security Manager for Removal](https://openjdk.org/jeps/411)
- [JEP 421: Deprecate Finalization for Removal](https://openjdk.org/jeps/421)
- [JEP 444: Virtual Threads](https://openjdk.org/jeps/444)
- [JEP 454: Foreign Function & Memory API](https://openjdk.org/jeps/454)
- [JEP 474: ZGC: Generational Mode by Default](https://openjdk.org/jeps/474)
