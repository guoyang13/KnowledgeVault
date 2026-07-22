既然你问到了 API，说明你已经准备好进入**“开发者/极客模式”**了！

只要你的电脑右上角挂着那个小羊驼图标，Ollama 就在你本地的 `http://localhost:11434` 端口默默运行着一个强大的 Web 服务器。

Ollama 提供的 API 主要分为**两大类**：一类是它**原生的 API**，另一类是极其重要的**“OpenAI 兼容 API”**。

以下是核心 API 接口的梳理及其实战用法（你可以直接复制 `curl` 命令到你的 Mac 终端里测试）：

---

### 第一类：OpenAI 兼容 API（最有用、最牛的杀手锏）

Ollama 完全复刻了 OpenAI 的接口格式。这意味着，任何支持 ChatGPT 的开源项目（比如 `hermes-agent`、LangChain、AutoGen），你只需要把接口地址改掉，一行代码都不用改，就能无缝接入。

#### 1. 聊天对话接口 (Chat Completions)
*   **接口地址：** `POST http://localhost:11434/v1/chat/completions`
*   **作用：** 和 ChatGPT 一模一样的多轮对话接口。
*   **测试命令（在终端直接跑）：**
```bash
curl http://localhost:11434/v1/chat/completions \
  -H "Content-Type: application/json" \
  -d '{
    "model": "qwen2.5:32b",
    "messages": [
      {
        "role": "user",
        "content": "请用一句话解释什么是量子力学？"
      }
    ]
  }'
```
*(注：把 `qwen2.5:32b` 换成你本地用 `ollama list` 查到的真实模型名字即可。)*

---

### 第二类：Ollama 原生核心 API

如果你想自己从零写一个脚本，直接调用 Ollama 原生 API 会更轻量、更简单。

#### 1. 基础生成接口 (Generate a completion)
*   **接口地址：** `POST /api/generate`
*   **作用：** 给一个提示词（Prompt），它直接吐出一段话（适合单次问答、代码续写）。
*   **测试命令：**
```bash
curl http://localhost:11434/api/generate -d '{
  "model": "qwen2.5:32b",
  "prompt": "为什么天空是蓝色的？",
  "stream": false
}'
```
*(注：`"stream": false` 表示等它全想好了再把整段话发给你。如果设为 `true`，它就会像打字机一样一个字一个字往外吐。)*

#### 2. 原生聊天接口 (Chat with a model)
*   **接口地址：** `POST /api/chat`
*   **作用：** 类似 OpenAI 的 chat 接口，支持上下文记忆。
*   **参数格式：** 也是传入 `model` 和带有 `role/content` 的 `messages` 数组。

---

### 第三类：模型管理 API（做自动化工具必备）

如果你想写一个后台管理面板，这些 API 能帮你直接管理本地的 AI 资产。

#### 1. 查看本地已下载的模型列表 (List Local Models)
*   **接口地址：** `GET /api/tags`
*   **测试命令：**
```bash
curl http://localhost:11434/api/tags
```
*(这会返回一个 JSON，里面包含你这台 Mac 上目前装了哪些模型、多大体积、哈希值是多少。)*

#### 2. 代码控制下载模型 (Pull a Model)
*   **接口地址：** `POST /api/pull`
*   **作用：** 你可以通过代码，让 Ollama 在后台自动去网上下载某个新模型。
*   **示例：** `{"name": "llama3.1:8b"}`

#### 3. 代码控制删除模型 (Delete a Model)
*   **接口地址：** `DELETE /api/delete`
*   **作用：** 释放硬盘空间。
*   **示例：** `{"name": "llama3.1:8b"}`
---

### 💡 给开发者的终极建议：如何优雅地调用？

虽然用 `curl` 测试很直观，但在实际写 Python 代码时，**你完全不需要手动去发 HTTP 请求。**

因为 Ollama 官方提供了非常好用的 **Python 库**。

**安装：**
```bash
pip install ollama
```

**在 Python 里调用（简直不要太简单）：**
```python
import ollama

# 就这三行代码，你就能在 Python 里拥有一个顶级 AI 助手！
response = ollama.chat(model='qwen2.5:32b', messages=[
    'content': '用 Python 写一个冒泡排序。',
  },
])
print(response['message']['content'])
```

**总结：** 配合你那强悍的 **M4 Max 芯片**，通过这些 API，你完全可以给自己开发一个“懂你所有本地文件代码、不用联网、反应极快的超级私人 AI 秘书”！