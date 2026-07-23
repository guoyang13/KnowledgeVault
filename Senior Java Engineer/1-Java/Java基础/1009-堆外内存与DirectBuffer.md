# 堆外内存与 DirectBuffer

> **[[1000-JVM专题导航]] · 模块二 · I/O 专题 · 02/02**  
> JVM 堆之外的 native 内存：分配原理、回收机制、与零拷贝/I/O 的关系。建议先读 [[1008-零拷贝]]。

## 阅读导航

```text
Part I  是什么           → 进程地址空间里堆外的位置
Part II 实现原理         → DirectByteBuffer、Unsafe、Cleaner
Part III 工程实践        → Netty 池化、容器 OOM、RocketMQ
```

← 上一篇 [[1008-零拷贝]] · ↑ [[1000-JVM专题导航]] · 下一篇 → [[1004-G1垃圾回收与堆内存]]

---

## 一句话概括

**堆外内存**由 OS 直接管理（`malloc`/`mmap`），JVM 通过 `DirectByteBuffer` 持有 native 指针；不参与 GC 扫描主体数据，适合 NIO、大缓冲区和 native I/O。

---

# Part I：是什么

## 1. JVM 进程内存布局（简化）

```text
Java Heap          ← -Xmx，GC 管理
Metaspace          ← 类元数据
Thread Stacks      ← -Xss
Code Cache         ← JIT
Direct Memory      ← -XX:MaxDirectMemorySize
其他 native        ← JNI、第三方库
```

| 对比 | 堆内 | 堆外 |
|------|------|------|
| 分配 | `new` / `byte[]` | `ByteBuffer.allocateDirect()` |
| 回收 | GC | Cleaner → `Unsafe.freeMemory` |
| I/O | 需拷贝到 Direct | 可直接作为 native I/O 源/目标 |

> `-Xmx` 只限制 Java 堆；容器 `limit` 需为堆外留余量。见 [[1004-G1垃圾回收与堆内存]]、`[[1005-ZGC垃圾回收与堆内存]]`。

---

# Part II：实现原理

## 1. 分配链路

```text
ByteBuffer.allocateDirect(n)
  → new DirectByteBuffer(n)
  → Bits.reserveMemory(n)          // 检查 MaxDirectMemorySize
  → Unsafe.allocateMemory(n)       // native malloc
  → Cleaner.create(deallocator)    // GC 回收 Java 对象时 free native
```

## 2. 读写

通过 `Unsafe.get/put*` 直接操作 `address + offset`，不经过 Java 数组边界检查。

## 3. 释放

- `DirectByteBuffer` Java 对象不可达 → GC → **Cleaner** 队列 → `Unsafe.freeMemory`
- **风险**：Java 对象很小，GC 不频繁 → native 泄漏；或 `-XX:MaxDirectMemorySize` 超限 → `OutOfMemoryError: Direct buffer memory`

## 4. MappedByteBuffer

`FileChannel.map()` → `mmap` 映射文件到堆外地址，是 `DirectByteBuffer` 子类，读写触发 OS page fault，由 Page Cache 支撑。

---

# Part III：工程实践

## 1. 为何 Netty / MQ 使用堆外

- **减轻 GC**：大 read buffer 不占用堆
- **减少拷贝**：堆内 `byte[]` 写 Socket 需先拷到 Direct（见 [[1008-零拷贝]]）
- **池化**：`PooledByteBufAllocator` 复用 Direct 块，避免频繁 malloc/free

## 2. RocketMQ 异步刷盘

- 开堆外：CommitLog 写路径更短，吞吐更高（极端丢约 200ms 数据）
- 关堆外：走堆内，丢约 500ms（见 MQ 专题笔记）

## 3. 容器部署注意

```text
limit 应 > Xmx + Metaspace + 线程栈 + Direct Memory + native 预估
```

排查 OOM 区分：`Java heap space` vs `Direct buffer memory` vs 容器 OOMKilled。

## 4. 使用建议

| 场景 | 建议 |
|------|------|
| 大网络 buffer | Direct + 池化 |
| 小临时 buffer | 堆内 `allocate()` 往往更省 |
| 文件映射随机读 | `MappedByteBuffer` |
| 必须及时释放 | 避免堆积大量 Direct 引用 |

---

## 与之相关

- [[1008-零拷贝]] — sendfile 与 Direct Buffer 拷贝链
- [[1006-JVM内存分配]] — 堆内 TLAB 分配
- [[Senior Java Engineer/计算机/深入理解计算机系统-CSAPP/09-虚拟内存|CSAPP 09 虚拟内存]] — 虚拟内存、Page Cache

## 外部参考

- [JDK 21 DirectByteBuffer](https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/DirectByteBuffer.html) — API 与堆外语义
- [Mechanical Sympathy: Direct buffers and zero copy](https://groups.google.com/g/mechanical-sympathy/c/A3WvNcpFjF0) — Direct vs sendfile 内核路径
- [It's all about buffers: zero-copy, mmap and Java NIO](https://shawn-xu.medium.com/its-all-about-buffers-zero-copy-mmap-and-java-nio-50f2a1bfc05c) — Heap vs Direct vs Mapped 对比

---

## 更新记录

- source: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/nio/DirectByteBuffer.html , https://groups.google.com/g/mechanical-sympathy/c/A3WvNcpFjF0
- updated: 2026-07-22
