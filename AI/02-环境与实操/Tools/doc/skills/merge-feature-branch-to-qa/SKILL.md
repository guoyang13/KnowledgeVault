---
name: merge-feature-branch-to-qa
description: Merges the current local feature branch into release/release-qa without pushing. Validates branch name feature/**, updates both branches, resolves merge conflicts, commits locally, and stops before push until the user confirms. After the user explicitly allows push and push succeeds, checkout back to the original feature/** branch. Use when the user asks to 合并开发分支到 QA、合并到 release-qa、feature 合入 QA、本地合并到 qa 分支、或执行本仓库的 QA 合并流程。
---

# 合并本地开发分支到 QA（release/release-qa）

在仓库根目录执行 Git 操作。目标：把当前 `feature/**` 开发分支合入 `release/release-qa`，**默认仅本地提交，不 push**（push 必须等用户明确确认）。用户确认并 push 成功后，**必须**再 checkout 回原来的 `feature/**` 开发分支。

## 前置假设

- 远程名一般为 `origin`；若项目不同，以 `git remote -v` 为准并替换下文中的 `origin`。
- QA 目标分支固定为 **`release/release-qa`**。

## 工作流

### 1. 校验当前分支并更新开发分支

1. 读取当前分支名：`git branch --show-current`（记为 `FEATURE_BRANCH`）。
2. **命名校验**：当前分支必须匹配 `feature/**`（例如 `feature/foo-bar`）。若不匹配，**停止**并说明：请先 checkout 到 `feature/**` 再执行本流程。
3. 将开发分支更新到最新（二选一，与团队习惯一致即可）：
   - `git fetch origin` 后 `git pull --ff-only`（或 `git pull origin FEATURE_BRANCH`）。
4. **记住** `FEATURE_BRANCH`（后续合并与说明中会用到）。

### 2. Checkout QA 分支并更新

1. `git checkout release/release-qa`
2. `git fetch origin`
3. `git pull --ff-only`（或 `git pull origin release/release-qa`），确保 `release/release-qa` 与远程一致后再合并。

### 3. 合并开发分支并处理冲突

1. `git merge FEATURE_BRANCH`（将第 1 步记下的分支名代入）。
2. 若发生冲突：
   - 按文件逐个解决冲突，删除冲突标记，保证编译与逻辑正确。
   - `git add` 已解决文件；全部解决后 **`git merge --continue`**（或若合并未进入进行中则直接 `git commit` 完成合并提交）。
3. 合并完成后可用 `git log -1 --oneline` 确认最新提交为合并提交或预期提交。

### 4. 提交、push 与用户确认，以及 push 后切回开发分支

- 若合并由 Git 自动完成且已生成合并提交，**不要**再重复无意义的空提交。
- 若仍需补充修改（例如冲突后的微调），在 `release/release-qa` 上正常 `git add` + `git commit`，信息写清楚来源分支与目的（示例：`merge feature/xxx into release/release-qa`）。
- **禁止**在本流程中执行 `git push`，除非用户**明确回复可以 push**。
- **未获 push 确认时**：流程结束时向用户说明当前已在 `release/release-qa`，本地已提交（或已合并提交），并列出可执行的 push 命令供其确认后自行或由代理执行，例如：`git push origin release/release-qa`。
- **用户明确确认可以 push 且已执行 push 成功后**（使用第 1 步记下的 `FEATURE_BRANCH`）：
  1. `git push origin release/release-qa`（或团队约定的远程与 refspec）。
  2. 确认 push 成功（命令退出码为 0、无致命错误）。
  3. **必须**切回原来的开发分支：`git checkout FEATURE_BRANCH`（将第 1 步记下的分支名代入）。
  4. 向用户说明：QA 分支已推送，当前工作分支已回到 `FEATURE_BRANCH`，便于继续开发。

## 异常与回滚提示

- 若在 `release/release-qa` 上合并后尚未 push，需要放弃本次合并：`git merge --abort`（合并进行中）或 `git reset --hard ORIG_HEAD`（**慎用**，会丢未 push 的本地提交）。
- 若 `pull` 与本地分叉严重，先与用户确认是使用 rebase、merge 还是重置策略，避免覆盖他人已推送的 QA 分支历史（本 skill 默认 **不** rewrite 已推送的共享分支，除非用户明确要求）。

## 自检清单

- [ ] 第 1 步分支名为 `feature/**` 且已拉最新
- [ ] `release/release-qa` 已 checkout 且已拉最新
- [ ] 合并完成、冲突已解决、工作区干净（`git status`）
- [ ] 已本地提交或已有合并提交
- [ ] 若尚未 push：已提示用户确认后再执行 push；若已 push：push 成功后已 `git checkout` 回 `FEATURE_BRANCH`
