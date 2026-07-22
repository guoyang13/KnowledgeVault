## 1. 目标与分工

|工具|负责什么|
|---|---|
|Obsidian（本仓库）|概念、路线、双链笔记（`.md`）|
|JupyterLab|可运行代码、图表、数据试验（`.ipynb`）|
|conda|隔离 Python 与 Jupyter 依赖|

原则：笔记用 Markdown，实验用 Notebook，两套内容可以互相链接，但不要混在同一套「全能环境」里。

---

## 2. 环境策略（推荐一种即可）

### 方案 A：单环境「够用」（推荐大多数人）

一个环境搞定 Jupyter + 常见数据分析：

jupyterlab # 环境名

├── python 3.12

├── jupyterlab

└── （按需）pandas, numpy, matplotlib, ipykernel

- 适合：学 AI、做小实验、读 CSV、画图
- 优点：简单，一条 `conda activate` 就够

### 方案 B：双环境「进阶」

|环境名|用途|
|---|---|
|`jupyterlab`|只装 JupyterLab + 轻量工具，启动快|
|`datascience`|数据分析 / 机器学习（pandas、sklearn、torch 等按需加）|

- 适合：以后包很多、怕装乱
- 用法：平时 `jupyter lab` 用 `jupyterlab`；重项目 `conda activate datascience` 再开

建议：你从知识库学习路线出发，先用方案 A；包变多再拆成 B。

---

## 3. 安装步骤（备忘，不执行）

创建环境（conda-forge）：

conda create -n jupyterlab python=3.12 jupyterlab -y -c conda-forge

常用数据包（可选，同一环境）：

conda activate jupyterlab

conda install pandas numpy matplotlib ipykernel -y -c conda-forge

启动：

conda activate jupyterlab

jupyter lab

不用时：

# 终端 Ctrl+C 停掉 Jupyter

conda deactivate

删除环境（重来时）：

conda env remove -n jupyterlab

与现有习惯一致：`auto_activate_base false` 可保留，需要时再 `conda activate jupyterlab`。

---

## 4. 在本仓库里的目录规划

在 `KnowledgeVault` 下单独划一块给可运行内容，和 `AI/` 笔记分开：

KnowledgeVault/

├── AI/ # 继续：知识体系、工具笔记（.md）

├── notebooks/ # 新建：所有 .ipynb

│ ├── 00-scratch/ # 临时试验、教程跟练

│ ├── 01-python-basics/

│ └── 02-data-viz/

└── .gitignore # 建议忽略见下文

- `AI/`：只放「怎么学、是什么」的文档（你已有的路线图、概念笔记等）
- `notebooks/`：所有「要跑起来」的东西
- 在 Obsidian 笔记里用 `[[../notebooks/xxx]]` 或普通路径引用笔记本即可

---

## 5. Git 与 Obsidian 注意点

建议在 `.gitignore` 里增加（若还没有）：

.ipynb_checkpoints/

__pycache__/

*.pyc

.DS_Store

# 大文件、数据集目录（按你实际路径改）

notebooks/**/data/

notebooks/**/*.csv

- 提交：`.ipynb` 可以进 Git，但 diff 会比较大；重要结论仍建议写回 `AI/` 的 `.md` 摘要
- Obsidian：不必为 Jupyter 装复杂插件；需要编辑笔记本时打开 JupyterLab 即可

---

## 6. 日常使用流程

打开终端conda activate jupyterlabjupyter lab浏览器编辑 .ipynb结论整理到 AI/*.md

1. `cd` 到仓库或 `notebooks/`
2. `conda activate jupyterlab`
3. `jupyter lab --notebook-dir=./notebooks`（可选，固定根目录）
4. 实验结束后，把要点、公式、结论抄到 Obsidian 的 Markdown（保持知识库可读、可搜）

固定项目根启动（推荐）：

cd /Users/guoyang/Documents/ObsidianVault/KnowledgeVault/KnowledgeVault

conda activate jupyterlab

jupyter lab --notebook-dir=./notebooks

---

## 7. 扩展与包：按需安装，不要一次装满

|阶段|安装什么|
|---|---|
|起步|仅 `jupyterlab` + `python`|
|学 pandas|`pandas numpy`|
|画图|`matplotlib`（或 `seaborn`）|
|机器学习|`scikit-learn`，再考虑 `pytorch`（单独评估体积）|
|多个环境共用同一 Jupyter|`ipykernel` + `python -m ipykernel install --user --name=xxx`（进阶，后期再弄）|

遇到装不上的包，优先查 conda-forge；个别库再 `pip install`（仍在已激活的 `jupyterlab` 环境里）。

---

## 8. 远程 / 本机无界面（可选）

jupyter lab --no-browser --port=8888

本机 SSH 转发后浏览器访问（需要时再细化）。

---

## 9. 环境备份（换机、重装）

装稳并试通后：

conda activate jupyterlab

conda env export -n jupyterlab > environment-jupyterlab.yml

新机器：

conda env create -f environment-jupyterlab.yml

---

## 10. 执行清单（你动手时按顺序打勾）

1.  `conda create -n jupyterlab python=3.12 jupyterlab -c conda-forge`
2.  `conda activate jupyterlab` → `jupyter lab` → 浏览器能打开
3.  在仓库下创建 `notebooks/00-scratch/`，新建一个测试笔记本
4.  （可选）`conda install pandas numpy matplotlib -c conda-forge`
5.  更新 `.gitignore`，避免 checkpoint、大数据进 Git
6.  （可选）`conda env export` 保存 `environment-jupyterlab.yml`

---

## 一句话

单独维护 `jupyterlab` 环境；Markdown 留在 `AI/`，可运行实验集中在 `notebooks/`；按需加数据科学包，结论回写到 Obsidian。