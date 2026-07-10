# ClassLoader 类加载机制

> **[[1000-JVM专题导航]] · 模块一 · 01/03**  
> 整理自 JVM / 类加载专题讨论。SPI 详见 [[1002-SPI机制]]，TCCL 详见 [[1003-TCCL线程上下文类加载器]]。

## 阅读导航

```text
Part I  基础模型    → 层次、加载步骤、双亲委派
Part II 实战场景    → MyClassLoader、AppClassLoader、Spring Boot、Tomcat
Part III 排查速记   → 验证清单、面试速记
```

↑ [[1000-JVM专题导航]] · 下一篇 → [[1002-SPI机制]]

## 一句话概括

[[ClassLoader]] 负责把 `.class` 字节码变成 JVM 中的 `Class` 对象。**双亲委派**写在 `loadClass()` 里；自定义加载通常只重写 `findClass()`。Spring Boot fat jar 用 [[LaunchedURLClassLoader]]，Tomcat WAR 用 [[WebappClassLoader]]。

---

## 1. 类加载器层次

```text
Bootstrap ClassLoader（C++ 实现，Java 中 getClassLoader() 为 null）
        ↑ 委派
Platform ClassLoader（JDK 9+，原 Extension 角色弱化）
        ↑ 委派
AppClassLoader / System ClassLoader（应用类加载器）
        ↑ 可选
自定义 ClassLoader（MyClassLoader、LaunchedURLClassLoader、WebappClassLoader）
```

| 加载器 | 加载什么 | `getClassLoader()` |
|--------|----------|-------------------|
| Bootstrap | `java.*`、`jdk` 核心类 | `null` |
| Platform | 部分 JDK 模块 | Platform 实例 |
| AppClassLoader | classpath 上的业务代码 + 依赖 jar + 资源 | AppClassLoader |
| 自定义 | 按实现决定（插件、BOOT-INF、WEB-INF） | 对应实例 |

获取应用类加载器：

```java
ClassLoader app = ClassLoader.getSystemClassLoader();
// 或
ClassLoader app = MyMain.class.getClassLoader();
```

---

## 2. 加载一个类的完整步骤

```text
loadClass(name)          ← 入口（委派逻辑在这里）
    ↓
findLoadedClass(name)    ← 查缓存，已加载则直接返回
    ↓
parent.loadClass(name)    ← 双亲委派：先问父加载器
    ↓（父失败）
findClass(name)          ← 从自定义路径读 .class 字节
    ↓
defineClass(...)         ← 字节码 → Class 对象
    ↓
resolveClass(c)          ← 链接（可选/延迟）
```

- **`loadClass`**：决定「找谁加载」
- **`findClass`**：决定「从哪读字节码」
- **`defineClass`**：真正把字节变成 `Class`（受保护方法）

类加载是 **懒加载**：第一次需要该类时才触发。

---

## 3. 双亲委派

### 3.1 标准流程（父先子后）

```text
收到 loadClass 请求
    ↓
1. 类已加载？→ 直接返回
    ↓
2. 先委派 parent.loadClass()
    ↓
3. 父加载器都失败 → 才调用自己的 findClass()
```

目的：

- 避免重复加载
- 保护核心类（如 `java.lang.String` 只能由 Bootstrap 加载）
- 保证同名类在 JVM 中只有一份（同一加载器命名空间内）

### 3.2 实现在 `loadClass`，不在 `findClass`

JDK `ClassLoader.loadClass` 核心逻辑（简化）：

```java
protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                if (parent != null) {
                    c = parent.loadClass(name, false);  // 先父
                } else {
                    c = findBootstrapClassOrNull(name);
                }
            } catch (ClassNotFoundException e) { }
            if (c == null) {
                c = findClass(name);  // 后子
            }
        }
        if (resolve) resolveClass(c);
        return c;
    }
}
```

### 3.3 正确自定义：只重写 `findClass`

```java
public class MyClassLoader extends ClassLoader {

    public MyClassLoader(ClassLoader parent) {
        super(parent);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        byte[] bytes = loadBytesFromDisk(name);
        return defineClass(name, bytes, 0, bytes.length);
    }
}
```

调用 `myLoader.loadClass("com.foo.Bar")` 时仍走父类 `loadClass`，**双亲委派保留**。

### 3.4 重写 `loadClass` 为何会破坏双亲委派

| 写法 | 后果 |
|------|------|
| 只调 `findClass`，不问 parent | 完全跳过委派，可重复定义类、伪造核心类 |
| 先 `findClass` 再 `super.loadClass`（子优先） | 故意打破标准委派（Tomcat、SPI 场景） |
| 重写时漏掉 `synchronized` / `findLoadedClass` | 并发下可能重复加载 |

**子优先示例（Tomcat / SPI）：**

```java
@Override
protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
    synchronized (getClassLoadingLock(name)) {
        Class<?> c = findLoadedClass(name);
        if (c == null) {
            try {
                c = findClass(name);              // ① 先自己
            } catch (ClassNotFoundException e) {
                c = super.loadClass(name, false); // ② 再双亲委派
            }
        }
        if (resolve) resolveClass(c);
        return c;
    }
}
```

---

## 4. MyClassLoader：何时加载、何时使用

### 4.1 两个概念

| 概念 | 含义 |
|------|------|
| **使用 ClassLoader** | 创建实例，调用 `loadClass` / `getResource` |
| **加载类** | 某类第一次需要时，`loadClass` → `findClass` → `defineClass` |

```text
new MyClassLoader()     → 只创建「工人」，尚未加载业务类
loader.loadClass(...)   → 此时才开始加载指定类
```

### 4.2 默认会被自动使用吗？

**不会。** 业务类默认由 **AppClassLoader** 加载。`MyClassLoader` 只有在以下情况才参与：

1. 代码显式 `new MyClassLoader()` 并 `loadClass`
2. 设为 **线程上下文类加载器（TCCL）**，SPI 等框架间接使用（详见 [[1003-TCCL线程上下文类加载器]]）
3. 容器/框架创建（Tomcat、插件框架）

`MyClassLoader` **这个类本身**在第一次被代码引用时，由 **父加载器（AppClassLoader）** 加载。

### 4.3 何时该用自定义 ClassLoader

| 需求 | 是否需要 |
|------|----------|
| 普通 Spring Boot CRUD | ❌ |
| 同 JVM 多版本 jar 隔离 | ✅ |
| 运行时插件 / 热部署 | ✅ |
| 从网络/磁盘动态加载类 | ✅ |

---

## 5. AppClassLoader 默认加载什么

**classpath 上的所有类和资源**：

- `target/classes` 编译出的业务 `.class`
- Maven/Gradle 依赖 jar
- `application.yml`、`mapper/*.xml` 等资源（`getResource`）

**不加载**：`java.*`（Bootstrap）、不在 classpath 上的类、其他 Tomcat WAR 的类。

粗算并发线程（阻塞模型）：`所需线程 ≈ QPS × 平均 RT（秒）`。

---

## 6. ClassLoader 自身何时被加载

打破「鸡生蛋」循环：

```text
① JVM 启动（C++）
      Bootstrap 加载 java.lang.Object、java.lang.ClassLoader、java.lang.String ...
      │
② JVM Java 层
      创建 PlatformClassLoader、AppClassLoader
      │
③ main 执行
      AppClassLoader 加载业务类；若引用 MyClassLoader 则加载之
      │
④ new MyClassLoader()
      实例创建，可加载其他类
```

| 对象 | 谁加载 / 谁创建 |
|------|----------------|
| `java.lang.ClassLoader` 类 | Bootstrap |
| `AppClassLoader` 实例 | JVM 启动代码 |
| `MyClassLoader` 类 | AppClassLoader（首次引用） |
| `MyClassLoader` 实例 | 你的 `new` |

```java
ClassLoader.class.getClassLoader();  // null → Bootstrap
String.class.getClassLoader();       // null
MyApp.class.getClassLoader();        // AppClassLoader 或子加载器
```

---

## 7. Spring Boot 打包与类加载

### 7.1 fat jar 结构

```text
myapp.jar
├── META-INF/MANIFEST.MF
│       Main-Class: org.springframework.boot.loader.JarLauncher
│       Start-Class: com.myapp.Application
├── org/springframework/boot/loader/    ← Loader（jar 根目录）
└── BOOT-INF/
    ├── classes/                        ← 业务代码 + 资源
    ├── lib/*.jar                       ← 全部依赖
    └── classpath.idx                   ← 可选，依赖顺序
```

运行时 classpath 等价于：`BOOT-INF/classes` + `BOOT-INF/lib/*`。

### 7.2 启动流程

```text
java -jar myapp.jar
    → AppClassLoader 加载 JarLauncher
    → JarLauncher 创建 LaunchedURLClassLoader
    → 加载 Start-Class，反射调用 main()
```

### 7.3 各类由谁加载

| jar 内位置 | 加载器 |
|------------|--------|
| `org/springframework/boot/loader/**` | AppClassLoader |
| `BOOT-INF/classes/**` | LaunchedURLClassLoader |
| `BOOT-INF/lib/*.jar` | LaunchedURLClassLoader |
| `java.*` | Bootstrap |

```java
// fat jar 运行典型输出
Application.class.getClassLoader();           // LaunchedURLClassLoader
JarLauncher.class.getClassLoader();           // AppClassLoader
String.class.getClassLoader();                // null
```

### 7.4 jar vs war

| 打包 | 运行 | 应用类加载器 |
|------|------|--------------|
| fat jar | `java -jar` | LaunchedURLClassLoader |
| war | 外置/内嵌 Tomcat | WebappClassLoader |
| IDE / `mvn spring-boot:run` | 开发运行 | AppClassLoader |

---

## 8. LaunchedURLClassLoader

- **来源**：Spring Boot Loader
- **继承**：`URLClassLoader`
- **能力**：解析嵌套 jar URL（`jar:file:/app.jar!/BOOT-INF/lib/xxx.jar!/`）
- **委派**：基本标准双亲委派（先父后子）
- **使用**：`java -jar` 时由 `JarLauncher` 自动创建，无需手写

```java
public class LaunchedURLClassLoader extends URLClassLoader {
    // 由 Launcher 创建，加载 BOOT-INF
}
```

Loader 打在 jar **根目录**的原因：若放在 `BOOT-INF/lib`，会「鸡生蛋」——没有 Loader 就无法读 BOOT-INF。

---

## 9. WebappClassLoader

- **来源**：Apache Tomcat
- **实现**：Tomcat 8+ 常用 `ParallelWebappClassLoader`（并行加载）
- **Spring Boot 内嵌**：`TomcatEmbeddedWebappClassLoader extends ParallelWebappClassLoader`
- **加载**：`WEB-INF/classes` + `WEB-INF/lib/*.jar`
- **委派**：**部分打破**双亲委派——Web 应用类 **子优先**，实现 WAR 间类隔离

```text
Tomcat 简化层次：

Bootstrap → System → Common/Catalina → WebappClassLoader(App A)
                                    → WebappClassLoader(App B)  // 隔离
```

处理 HTTP 请求时，TCCL 常为 WebappClassLoader，SPI 等会间接用到。TCCL 产生时机、异步错位、错 ClassLoader 导致异常详见 [[1003-TCCL线程上下文类加载器]]。

---

## 10. 验证与排查

```java
public class LoaderDemo {
    public static void main(String[] args) {
        System.out.println("ClassLoader 类: " + ClassLoader.class.getClassLoader());
        System.out.println("当前类:       " + LoaderDemo.class.getClassLoader());
        System.out.println("系统加载器:   " + ClassLoader.getSystemClassLoader());
        System.out.println("TCCL:         " + Thread.currentThread().getContextClassLoader());
    }
}
```

| `getClassLoader()` 结果 | 含义 |
|-------------------------|------|
| `null` | Bootstrap 加载 |
| `AppClassLoader` | classpath 默认 |
| `LaunchedURLClassLoader` | Spring Boot fat jar |
| `ParallelWebappClassLoader` | Tomcat WAR |

---

## 11. 常见面试速记

1. **双亲委派**：`loadClass` 先父后子；改 `findClass` 不改委派。
2. **破坏委派**：重写 `loadClass` 且跳过 parent，或子优先（Tomcat）。
3. **MyClassLoader 默认不用**：必须显式使用或框架创建。
4. **Bootstrap 与 null**：核心类 `getClassLoader()` 返回 null。
5. **Spring Boot jar**：Loader 在根目录由 App 加载；业务在 BOOT-INF 由 LaunchedURLClassLoader 加载。
6. **SPI 与 TCCL**：双亲委派下 JDK 类无法直接加载应用 jar 中的实现，靠 TCCL + `ServiceLoader`（详见 [[1002-SPI机制]]、[[1003-TCCL线程上下文类加载器]]）。
7. **异步 TCCL**：线程池 Worker 在创建时固定 TCCL，submit 不会自动更新。
8. **错 ClassLoader**：在错误 classpath 上 loadClass → ClassNotFound / SPI 失效 / ClassCastException。
9. **ClassLoader 谁加载**：`ClassLoader` 类由 Bootstrap 在 JVM 最早阶段加载。

---

## 12. 相关链接

- [[1000-JVM专题导航]] — 专题索引与学习路径
- [[1002-SPI机制]] — ServiceLoader、META-INF/services、JDBC
- [[1003-TCCL线程上下文类加载器]] — TCCL 产生时机、异步坑、错 CL 导致异常
- [[JDK8到JDK23-JVM重要变化]] — Metaspace、JFR、模块化
- Spring Boot 源码：`LaunchedURLClassLoader`、`JarLauncher`（本仓库 `spring-boot-2.6.14`）

---

## 更新记录

- 2026-07-11：初版，整理 ClassLoader 专题讨论
- 2026-07-11：补充 SPI、TCCL 交叉引用
