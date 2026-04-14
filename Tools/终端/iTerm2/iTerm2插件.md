要把 iTerm2 配置成那种带**图标（如 Git 分支图标、文件夹图标）**、**丰富颜色**和**实时状态**的样子，目前主流且最稳健的方案是：
**iTerm2 + Oh My Zsh + Powerlevel10k 主题 + Nerd Fonts 字体**。

以下是保姆级配置步骤：

---

### 第一步：安装 Oh My Zsh
如果你还没安装，打开 iTerm2 输入：
```bash
sh -c "$(curl -fsSL https://raw.githubusercontent.com/ohmyzsh/ohmyzsh/master/tools/install.sh)"
```
安装完成后，你会发现终端颜色稍微变了一下。

---

### 第二步：安装 Nerd Fonts 字体（核心：解决图标乱码）
那些漂亮的图标其实是特殊的字体字符。如果没有安装 Nerd Fonts，图标会显示成“问号”或“方块”。

1.  **下载字体**：推荐下载 **MesloLGS NF**，它是专门为这款主题优化的。
    *   [MesloLGS NF Regular.ttf](https://github.com/romkatv/powerlevel10k-media/raw/master/MesloLGS%20NF%20Regular.ttf) (点击直接下载)
2.  **安装**：下载后双击打开，点击“安装字体”。
3.  **在 iTerm2 中应用**：
    *   打开 iTerm2 设置：`Cmd (⌘)` + `,`
    *   进入 `Profiles` -> `Text` -> `Font`。
    *   点击下拉框，选择 **MesloLGS NF**。

---

### 第三步：安装 Powerlevel10k 主题
这是目前最流行的、带图标效果最好的主题。

1.  **下载主题**：
    ```bash
    git clone --depth=1 https://github.com/romkatv/powerlevel10k.git ${ZSH_CUSTOM:-$HOME/.oh-my-zsh/custom}/themes/powerlevel10k
    ```
2.  **修改配置文件**：
    输入 `nano ~/.zshrc` 或者用你喜欢的编辑器打开这个文件。
3.  **找到 `ZSH_THEME` 这一行**，修改为：
    ```bash
    ZSH_THEME="powerlevel10k/powerlevel10k"
    ```
4.  保存退出（Nano 编辑器按 `Ctrl+O` 回车，再按 `Ctrl+X`）。
5.  **重启终端**（或输入 `source ~/.zshrc`）。

---

### 第四步：交互式配置（“变美”的瞬间）
重启 iTerm2 后，你会自动进入 Powerlevel10k 的配置向导。它会问你一系列问题：
1.  **能不能看到钻石/锁/分支图标？**（如果第二步字体设置对了，这里应该选 Yes）。
2.  **想要什么风格？** (Rainbow 彩虹色、Lean 精简风、Classic 经典款)。
3.  **是否显示当前时间？**
4.  **两行显示还是单行显示？**

**如果不小心退出了，或者以后想重新换个风格，输入 `p10k configure` 即可重新进入。**

---

### 第五步：添加“自动补全”和“语法高亮”插件（进阶推荐）
让你的 iTerm2 像 Warp 一样好用：

1.  **语法高亮**（让命令变颜色，输错显示红色）：
    ```bash
    git clone https://github.com/zsh-users/zsh-syntax-highlighting.git ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-syntax-highlighting
    ```
2.  **自动补全**（根据历史记录自动预测）：
    ```bash
    git clone https://github.com/zsh-users/zsh-autosuggestions ${ZSH_CUSTOM:-~/.oh-my-zsh/custom}/plugins/zsh-autosuggestions
    ```
3.  **启用插件**：
    再次打开 `nano ~/.zshrc`，找到 `plugins=(git)` 这一行，改成：
    ```bash
    plugins=(git zsh-syntax-highlighting zsh-autosuggestions)
    ```
    保存并重启终端。

---

### 最终效果：
配置完成后，你的 iTerm2 会：
*   **显示 Git 分支**（比如 `main`），并且会用颜色告诉你是否有未提交的文件。
*   **显示路径图标**（如文件夹小图标）。
*   **显示 Python/Nodejs 环境**。
*   **命令输错会变红**。
*   **灰度预测命令**，按右方向键直接补全。

**如果你在配置过程中看到任何乱码， 99% 的原因都是 iTerm2 的字体设置里没有选对那个带 "NF" 后缀的字体。**