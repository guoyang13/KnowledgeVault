# 一、java开发环境配置

## 1、jdk和maven配置

可在settings.json文件中进行操作, Cmd+Shift+P(Mac)打开命令面板,输入"Preferences:Open User Settings(JSON) 并回车,打开settings.json文件

```json
{
    "java.home": "/Users/guoyang/Library/Java/JavaVirtualMachines/openjdk-23.0.1/Contents/Home",
    "java.configuration.maven.userSettings": "/Users/guoyang/DeveloperTools/apache-maven-3.9.9/conf/settings-BO.xml",
}
```



## 2、java插件

Cmd+Shift+X(Mac)输入Extension Pack for Java



### 3、Environment variables配置

在当前项目路径下 /.vscode/launch.json

```json
{
    // Use IntelliSense to learn about possible attributes.
    // Hover to view descriptions of existing attributes.
    // For more information, visit: https://go.microsoft.com/fwlink/?linkid=830387
    "version": "0.2.0",
    // 调试启动时弹出列表，选择 network.environment（可增删 options）
    "inputs": [
        {
            "id": "networkEnvironment",
            "type": "pickString",
            "description": "选择 network.environment",
            "options": [
                "internal",
                "external"
            ],
            "default": "internal"
        }
    ],
    "configurations": [
        
        {       
            "type": "java",
            "name": "Current File",
            "request": "launch",
            "mainClass": "${file}"
        },
        {
            "type": "java",
            "name": "Application",
            "request": "launch",
            "mainClass": "com.bo.rt.biz.scm.procurement.application.Application",
            "projectName": "rt-biz-scm-procurement-service-starter",
            "env": {
                "network.environment": "${input:networkEnvironment}",
                "spring.cloud.nacos.discovery.group": "guoyang"
            }
        }
    ]
}
```



# 二、插件



