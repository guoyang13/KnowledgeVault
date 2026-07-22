---
name: data-dict-json
description: Builds a JSON array of data-dict options from Java enum sources in the shape [{"dictValue":"...","dictLabel":"..."}]. Use when the user asks for 数据字典 json, dict json from enum, frontend dict options, or OpenAPI/init data derived from an enum class they name or open.
---

# 数据字典 JSON（枚举 → dictValue / dictLabel）

## 目标

根据用户**指定的一个或多个 Java 枚举类**（路径或类名），生成用于前端/配置初始化等场景的 JSON，**严格**为对象数组：

```json
[{"dictValue":"枚举code","dictLabel":"枚举名称"}]
```

- `dictValue`：业务侧存库、传参用的 **code**（与枚举常量名可不同）。
- `dictLabel`：展示用 **名称/描述**（一般为中文或可读文案）。

输出时默认给出**单行紧凑 JSON**（便于粘贴）；若用户要求格式化，再换行缩进。

## 执行步骤

1. **定位源码**：读取用户给出的枚举 `.java` 文件（或在工作区按全限定名搜索）。不要使用过时或未提交的臆测。
2. **遍历常量**：按源码中**声明顺序**输出；跳过非业务项（例如若存在仅占位的 `UNKNOWN` 且类注释标明不对外，仅当用户要求排除时再排除）。包含 `@Deprecated` 的项在输出可单独一行注释说明，默认仍输出除非用户不要。
3. **映射规则**（按优先级）：
   - **双字段构造**（本仓常见：`private final String code` + `String desc`，或 `Integer code` + `String desc` 等）：  
     - `dictValue` = 该常量构造调用中的 **code 实参**（`Integer`/`Byte`/`Long` 等转为 **JSON 字符串**，与示例 `"...code"` 一致；若用户明确要求数字型 `dictValue`，再输出不带引号的数字）。  
     - `dictLabel` = **desc / name / label** 等与「展示名」对应的实参（以类中字段名为准）。
   - **仅常量名、无 code/desc 字段**：`dictValue` = 枚举常量标识符（`name()`）；`dictLabel` = 紧邻常量上的 JavaDoc 首行去 `*` 后_trim_，若无则同 `dictValue`。
   - **与框架接口相关**：若实现 `StringBaseEnum` / `IntegerBaseEnum` 等，仍以构造参数中的 code、描述字段为准，与项目存库一致。
4. **转义**：`dictLabel` 含引号、反斜杠时按 JSON 规则转义。
5. **交付**：只给出 JSON（及必要时一句说明）；不要擅自改成 `'value'/'label'` 等其它键名，除非用户明确改字段名。

## 示例（本仓库风格）

输入：`ECommerceChannelEnum.java` 中 `LAZADA("LAZADA", "Lazada")` 等。

输出：

```json
[{"dictValue":"B_END","dictLabel":"B端"},{"dictValue":"LAZADA","dictLabel":"Lazada"},{"dictValue":"SHOPEE","dictLabel":"Shopee"},{"dictValue":"TIKTOK","dictLabel":"Tiktok"},{"dictValue":"INDEPENDENT_SITE","dictLabel":"独立站"},{"dictValue":"KOL_LIVE_STREAMING","dictLabel":"达人直播"},{"dictValue":"OTHER","dictLabel":"其他"}]
```

## 额外说明

- 多个枚举类：用户若要求「多个合成一个数组」，须说明是否加 `dictType` 等分组字段；**默认**为「每个枚举单独一段 JSON」，避免键结构变化。
- 用户若指定键名为 `value`/`label` 等，在回复中说明已替换字段名并仅在该场景下偏离上述模板。
