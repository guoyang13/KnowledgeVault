# macOS 安装 MinerU

> 便于在 Obsidian 中查阅。若尚未安装 conda，可先完成：[[macOS命令行安装Miniconda]]

## 环境与版本要求（以官方文档为准）

- **操作系统**：macOS 建议 **14.0 及以上**（见 [MinerU Quick Start](https://opendatalab.github.io/MinerU/quick_start/)）。
- **Python**：**3.10–3.13**。
- **资源**：全量安装 `mineru[all]` 建议充足 **内存** 与 **磁盘**（官方表格常写 16GB+ 内存、20GB+ 磁盘量级，视版本调整）。
- **后端**：无合适 GPU 时可用纯 CPU 的 **`pipeline`** 后端（见下文运行示例）。

## 推荐：conda 独立环境内安装

避免污染 `base`：

```bash
conda create -n mineru python=3.11 -y
conda activate mineru
python -m pip install --upgrade pip
pip install -U "mineru[all]"
```

**国内 PyPI 镜像（可选）：**

```bash
pip install -U "mineru[all]" -i https://pypi.tuna.tsinghua.edu.cn/simple
```

Python 版本可将 `3.11` 换成 `3.10` / `3.12` / `3.13`（以当前 conda 可解析为准）。

## 已有虚拟环境时

在**已激活**的 conda / venv 中执行：

```bash
python -m pip install --upgrade pip
pip install -U "mineru[all]"
```

与 Miniconda 是否通过图形界面安装无关。

## 安装后：通常还需下载模型

`pip install` 成功 **≠** 立刻能解析。首次使用一般需要**下载模型权重**（体积大、需联网）。请查看当前版本文档中的模型下载说明，例如：

```bash
mineru-models-download --help
```

配置与模型源详见官方：[Model Source](https://opendatalab.github.io/MinerU/usage/model_source/)。国内访问 HuggingFace 不稳定时，文档常建议切换到 **ModelScope** 等源，例如：

```bash
export MINERU_MODEL_SOURCE=modelscope
```

（以官方最新说明为准。）

## 验证与基本用法

```bash
mineru --help
```

解析示例（路径按需替换）：

```bash
mineru -p <输入_PDF_图片或_DOCX_或目录> -o <输出目录>
```

纯 CPU 或需指定后端时（参数以 `--help` 为准）：

```shell
mineru -p <输入路径> -o <输出目录> -b pipeline
```

更多用法：[Usage](https://opendatalab.github.io/MinerU/usage/)





```bash
mineru -p "/Users/guoyang/Documents/Obsidian Vault/MyKnowledge/java基础/01-项目性能优化" \
  -o "/Users/guoyang/Documents/Obsidian Vault/MyKnowledge/java基础/01-项目性能优化-md" \
  -b vlm-auto-engine
```



---

**标签**：`#MinerU` `#conda` `#pip` `#macOS`
