
GitHub 在 2021 年就停止支持使用账号密码推送代码了。以下是针对 GitHub 的解决步骤：

### 一、 如何看 GitHub 的 Username？
1. 点击 GitHub 右上角的**个人头像**。
2. 菜单中加粗的名字是你的昵称，**下方以 `@` 开头或紧跟在后面的名字**才是你的 **Username**。
3. 或者点击 **Your profile**，地址栏 `github.com/` 后面那一串就是你的用户名。

---

### 二、 在 GitHub 生成 Access Token (PAT)

由于不能用密码，你需要生成一个 `ghp_` 开头的令牌：

1.  点击右上角**头像** -> **Settings**（设置）。
2.  在左侧菜单栏拉到**最底部**，点击 **Developer settings**（开发者设置）。
3.  点击 **Personal access tokens** -> 选择 **Tokens (classic)**。
    *   *注意：不建议选 Fine-grained，除非你有特殊权限控制需求，Classic（经典版）最简单。*
4.  点击 **Generate new token** -> 选择 **Generate new token (classic)**。
5.  **Note**: 随便填个名字（如 `my-laptop`）。
6.  **Expiration**: 建议选 `No expiration`（永不过期）或更长的时间。
7.  **Select scopes (权限)**：**务必勾选 `repo`**（这会选中下面所有关于仓库的操作）。
8.  点击页面最下方的 **Generate token**。
9.  **【核心一步】**：复制那个以 `ghp_` 开头的长字符串。**关掉页面它就消失了，请务必先找个地方存一下。**

---

### 三、 如何在本地使用这个 Token

当你再次回到终端执行 `git push` 时：

1.  **Username**: 输入你的 GitHub 用户名。
2.  **Password**: **粘贴你刚才复制的 `ghp_...` 令牌**（在终端输入时不会显示任何字符，直接粘贴后按回车即可）。

---

### 四、 如果电脑不弹出输入框，直接报错怎么办？

如果报错 `Invalid username or token` 而不让你重新输入，说明你的电脑记录了旧的错误信息。你需要清除它：

*   **Windows 用户：**
    1.  搜索并打开“凭据管理器 (Credential Manager)”。
    2.  点击“Windows 凭据”。
    3.  找到 `git:https://github.com`。
    4.  点击“编辑”，在密码栏粘贴你的 **Token**；或者直接“删除”，然后重新执行 push 就会弹出输入框了。

*   **Mac 用户：**
    1.  打开“钥匙串访问 (Keychain Access)”。
    2.  搜索 `github.com`。
    3.  删除相关的“互联网密码”条目。
    4.  重新执行 push，输入用户名和 Token。

---

### 五、 终极方案：配置 SSH（强烈推荐）

如果你不想每次都折腾 Token，配置 SSH 是以后最省事的方法：

1.  **本地生成密钥：** `ssh-keygen -t ed25519`（一路回车）。
2.  **复制公钥：** `cat ~/.ssh/id_ed25519.pub`。
3.  **在 GitHub 绑定：** **Settings** -> **SSH and GPG keys** -> **New SSH key**，把内容粘进去。
4.  **修改仓库链接：** 
    ```bash
    git remote set-url origin git@github.com:用户名/项目名.git
    ```
    以后你再 push，**任何密码都不需要了**。