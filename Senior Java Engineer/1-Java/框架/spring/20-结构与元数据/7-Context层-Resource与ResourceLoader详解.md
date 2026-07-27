---
type: canonical
status: reviewed
topic: Spring Resource / ResourceLoader
source_version: 6.2.x
aliases:
  - Spring Resource
  - Spring 资源抽象
---

# Resource 与 ResourceLoader 详解

## 摘要

Spring 资源抽象（Resource Abstraction）用统一 API 表示 classpath、文件系统、URL、Servlet 上下文和内存中的资源。`Resource` 表示资源句柄，`ResourceLoader` 根据位置字符串解析句柄；它们不保证资源一定存在，也不等同于已经打开的输入流。

## 1. 核心类型

| API | 中文定位 | 关键能力 |
| --- | --- | --- |
| `Resource` | 资源描述符 | 存在性、URL/URI、文件名、输入流、相对资源 |
| `ResourceLoader` | 资源加载器 | `getResource(location)`、类加载器访问 |
| `ResourcePatternResolver` | 资源模式解析器 | 根据模式返回多个资源 |
| `EncodedResource` | 带编码的资源包装 | 以指定 Charset 读取文本 |

常见实现：

| 实现 | 典型来源 |
| --- | --- |
| `ClassPathResource` | classpath |
| `FileSystemResource` | 文件系统 |
| `UrlResource` | URL |
| `ServletContextResource` | Web 应用上下文 |
| `ByteArrayResource` | 内存字节数组 |
| `InputStreamResource` | 已有输入流 |

## 2. 位置字符串如何解析

```text
classpath:config/app.xml
  -> ClassPathResource

file:/opt/app/config.yml
  -> FileSystemResource 或 URL 语义的文件资源

https://example.com/schema.json
  -> UrlResource

无前缀位置
  -> 由具体 ResourceLoader 的上下文决定
```

`ApplicationContext` 本身实现 `ResourceLoader`。因此无前缀路径在 `ClassPathXmlApplicationContext` 与 Web 上下文中可能具有不同语义。跨上下文代码应优先使用明确前缀。

## 3. 单资源与模式资源

`ResourceLoader#getResource` 返回一个资源句柄，不执行通配符批量解析。

`ResourcePatternResolver#getResources` 支持模式；常见 `classpath*:` 用于搜索 classpath 中多个匹配位置：

```java
Resource[] resources =
    resolver.getResources("classpath*:META-INF/spring/*.xml");
```

模式解析结果受 ClassLoader、打包方式和协议处理能力影响，不能假设所有资源都可转换为 `File`。

## 4. 资源不是 File

资源可能位于 JAR、远程 URL 或内存中：

```java
try (InputStream input = resource.getInputStream) {
    // consume stream
}
```

只有底层确实是文件系统资源时，`resource.getFile` 才可靠。通用库代码应优先使用 `getInputStream`、`getURL` 或 `getURI`，并负责关闭自己打开的流。

## 5. 注入与使用

```java
@Component
class TemplateReader {
    private final Resource template;

    TemplateReader(@Value("classpath:templates/order.txt") Resource template) {
        this.template = template;
    }
}
```

字符串到 `Resource` 的转换由 Spring 类型转换基础设施参与。也可以注入 `ResourceLoader`，让对象在运行时按位置解析资源。

## 6. 常见误区

| 误区 | 修正 |
| --- | --- |
| `Resource` 就是文件 | 它可能来自 JAR、URL、内存或 ServletContext |
| 得到 `Resource` 就代表资源存在 | 句柄可先创建，读取时才失败 |
| 所有资源都能 `getFile` | 非文件系统资源通常不能 |
| `classpath:` 会返回所有同名资源 | 批量搜索通常使用 `classpath*:` 和 Pattern Resolver |
| 无前缀路径含义固定 | 由具体 `ResourceLoader` 决定 |

关联：[[5-Context层-ApplicationContext详解]] · [[6-Context层-Environment与PropertySource详解]]
