# DroidCast_raw

本项目修改自 [DroidCast](https://github.com/rayworks/DroidCast) ，是为 [AzurLaneAutoScript](https://github.com/LmeSzinc/AzurLaneAutoScript) 提供的一个能够在 Android 设备上面截取屏幕并**返回 Bitmap** 的工具。

⚠️ 代码中通过反射调用了一些系统隐藏的方法，相关功能可能会随着 Android 系统接口的变化而受到影响。

当前该工具支持从SDK 23(Android 6.0) 至 SDK33(Android 13)的屏幕截图获取。

## 依赖

 - [Python 3.6+](https://www.python.org/downloads/)（可选）

 - [ADB Tools](https://developer.android.google.cn/studio/releases/platform-tools)

## 快速上手

 - 连接你的 Android 设备或模拟器

 - 安装 APK

```shell
adb install DroidCast_raw-release-1.0.apk
```

 - 运行脚本

> 默认运行在 53516 端口上

```shell
python scripts/automation.py
```

> 你可以通过附加参数 ```-h``` 来获取帮助，如```python scripts/automation.py -h```

随后，默认浏览器将会打开，展示屏幕截图。

 - （可选）指定截屏服务端口

```shell
python scripts/automation.py -p 8080
```

## 手动运行

> 注：安装 APK 后，你可以使用脚本来自动化完成以下操作

### 方法一（需要安装 APK）

 - 安装 APK

```shell
adb install DroidCast_raw-release-1.0.apk
```

 - 获取 ```CLASSPATH```

```shell
adb shell pm path ink.mol.droidcast_raw
```

返回的 ```path:``` 后面的内容即为 ```CLASSPATH```

 - 通过 ```app_process``` 启动内部的图片处理服务进程

```shell
adb shell $CLASSPATH app_process / ink.mol.droidcast_raw.Main (--port=8080)
```

 - 使用 ```adb forward``` 命令将本地（PC）socket 连接重定向到已连接的 Android 设备上

```shell
adb forward tcp:53516 tcp:53516
```

此时可打开 ```http://127.0.0.1:53516/preview``` 获取截图

### 方法二（无需安装 APK）

 - 将 apk 文件 push 到手机的 tmp 文件夹下

```
adb push DroidCast_raw-release-1.0.apk /data/local/tmp
```

 - 通过 app_process 启动内部的图片处理服务进程

```shell
adb shell /data/local/tmp/DroidCast_raw-release-1.0.apk app_process / ink.mol.droidcast_raw.Main (--port=8080)
```
> 注： 在某些设备上, 如果碰到类似 ```appproc: ERROR: could not find class 'ink.mol.droidcast_raw.Main'``` 的错误，请改用方法一。

 - 使用 ```adb forward``` 命令将本地（PC）socket 连接重定向到已连接的 Android 设备上

```shell
adb forward tcp:53516 tcp:53516
```

此时可打开 ```http://127.0.0.1:53516/preview``` 查看截图或者按指定的图片大小和类型查看，如 ```http://127.0.0.1:53516/preview?width=1080&height=1920```

## 数据返回
```shell
GET http://ip:port/screenshot(?width=xxx&height=xxx)
返回 BitmapByteArray(RGB_565) ContentType="application/octet-stream"
```

```shell
GET http://ip:port/preview(?width=xxx&height=xxx)
返回 ByteArrayOutputStream ContentType="image/png"
```

## 参考

[DroidCast](https://github.com/rayworks/DroidCast)

[vysor 原理以及 Android 同屏方案](https://juejin.im/entry/57fe39400bd1d00058dd4652)

[scrcpy : Display and control your Android device](https://github.com/Genymobile/scrcpy)

## 相关项目

[scrcpy](https://github.com/Genymobile/scrcpy)

[web-adb](https://github.com/mfinkle/web-adb)

[AndroidScreenShot_SysApi](https://github.com/weizongwei5/AndroidScreenShot_SysApi)

## License

```
Copyright (C) 2023 Torther

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
