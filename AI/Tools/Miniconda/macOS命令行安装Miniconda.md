# macOS 命令行安装 Miniconda

> 便于在 Obsidian 中查阅。MinerU 安装见：[[macOS安装MinerU]]

## 前提

- 已删除旧 Miniconda 目录（或确认可覆盖安装到目标路径）。
- 先确认 CPU 架构，安装包二选一：
  - **Apple Silicon (M 系列)**：安装包名含 `arm64`
  - **Intel**：安装包名含 `x86_64`

在终端执行：

```bash
uname -m
```

输出 `arm64` 使用 arm64 脚本；输出 `x86_64` 使用 x86_64 脚本。

## 步骤 1：下载官方安装脚本

将 URL 中的架构文件名换成与你的 `uname -m` 对应的包。

**Apple Silicon：**

```bash
cd ~
curl -LO https://repo.anaconda.com/miniconda/Miniconda3-latest-MacOSX-arm64.sh
```

**Intel：**

```bash
cd ~
curl -LO https://repo.anaconda.com/miniconda/Miniconda3-latest-MacOSX-x86_64.sh
```

参考：[Miniconda - Conda 文档](https://docs.conda.io/en/latest/miniconda.html)

## 步骤 2：静默安装到用户目录

`-b` 为批处理（非交互），`-p` 指定安装路径（常用 `~/miniconda3`）。

**arm64 示例：**

```bash
bash ~/Miniconda3-latest-MacOSX-arm64.sh -b -p "$HOME/miniconda3"
```

**x86_64 示例：**

```bash
bash ~/Miniconda3-latest-MacOSX-x86_64.sh -b -p "$HOME/miniconda3"
```

安装完成后可删除安装脚本：

```bash
rm ~/Miniconda3-latest-MacOSX-arm64.sh
# 或
rm ~/Miniconda3-latest-MacOSX-x86_64.sh
```

## 步骤 3：初始化 shell（zsh）

```bash
"$HOME/miniconda3/bin/conda" init zsh
```

关闭并重新打开终端，或执行：

```bash
source ~/.zshrc
```

## 步骤 4：验证

```bash
conda --version
conda info
```

确认 `base` 对应路径为 `~/miniconda3`（或与 `-p` 一致的路径）。

## 建议：避免污染 base

- 尽量不要在 `base` 环境中长期执行大体积的 `pip install`。
- 为每个项目或工具（如 MinerU）单独 `conda create -n 环境名`。

---

**标签**：`#conda` `#miniconda` `#macOS`
