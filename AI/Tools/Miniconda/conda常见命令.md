### 安装
```bash
mkdir -p ~/miniconda3
curl https://repo.anaconda.com/miniconda/Miniconda3-latest-MacOSX-arm64.sh -o ~/miniconda3/miniconda.sh
bash ~/miniconda3/miniconda.sh -b -u -p ~/miniconda3
rm ~/miniconda3/miniconda.sh
```

### 初始化 Zsh
```bash
~/miniconda3/bin/conda init zsh
source ~/.zshrc
```

### 架构师强迫症专属小贴士（可选）
```bash
conda config --set auto_activate_base false
```

### 激活环境
```bash
conda activate
conda activate mineru
```

### 退出 Conda 的虚拟环境

```bash
conda deactivate
```

### 删除环境

```bash
conda env remove -n mineru
```



