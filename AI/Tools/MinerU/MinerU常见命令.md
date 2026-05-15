## 新建干净沙盒环境（Python 3.10～3.13）

```bash
conda create -n mineru python=3.11 -y #(-y表示默认同意)

# 激活环境
conda activate mineru

# 升级 pip
python -m pip install --upgrade pip
```



## 安装 MinerU（全量依赖）

```bash
# 官方写法
pip install -U "mineru[all]"

# 清华镜像
pip install -U "mineru[all]" -i https://pypi.tuna.tsinghua.edu.cn/simple

# 验证
mineru --help

python -c "import mineru; print('ok')"

```





### 下载模型

设置国内加速节点（由于官网在国外，国内直接下必报错，先输入这行）：

  ```bash
   export HF_ENDPOINT=https://hf-mirror.com
  ```

**触发一键下载命令：**

```bash
pip install modelscope
```

**触发下载模型文件**

```bash
python -c "from modelscope import snapshot_download; snapshot_download('opendatalab/pdf_extract_models', local_dir='~/magic-pdf/models')"
```



### 开始转换你的第一份 PDF！

```bash
# 请将下面的路径替换为你真实的路径
magic-pdf -p /你的PDF文件所在路径/test.pdf -o /你想要输出的文件夹路径/ -m auto
```

(参数解释：-p 是输入文件，-o 是输出目录，-m auto 表示让 AI 自动分析这篇 PDF 是单栏还是双栏排版。)



### Mac 用户的避坑与心理建设

1. **速度会比较慢：** 因为 Mac 没有 Nvidia 的独立显卡，跑这种重量级视觉模型纯靠 CPU（或者 Apple 芯片）。转换一页排版复杂的 PDF 可能需要 10 秒到 30 秒。如果是 100 页的书，请把它挂在后台去喝杯咖啡。
   
2. **结果验收（联动 Obsidian）：** 转换完成后，去桌面的 pdf_output 文件夹里，你会发现它为你建好了一个带 .md 文件和所有裁剪好图片的完整目录。**把这个完整的目录直接拖进你的 Obsidian 仓库里**，你就可以开始构建这本神作的个人知识库了！