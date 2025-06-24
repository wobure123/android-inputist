# 通用输入改写助手 - 快速测试指南

## 🚀 快速测试方案

### 方案一：本地Android Studio测试（推荐）

#### 1. 环境检查
```bash
# 检查Java版本（需要JDK 8+）
java -version

# 检查Android SDK
echo $ANDROID_HOME

# 检查连接的设备
adb devices
```

#### 2. 一键构建和安装
```bash
# 使用项目提供的构建脚本
./build.sh
```

### 方案二：命令行快速构建

#### 1. 清理和构建
```bash
# 清理项目
./gradlew clean

# 构建Debug APK
./gradlew assembleDebug

# 检查构建结果
ls -la app/build/outputs/apk/debug/
```

#### 2. 安装到设备
```bash
# 安装APK
adb install app/build/outputs/apk/debug/app-debug.apk

# 启动应用
adb shell am start -n "com.inputist.universal/.ui.MainActivity"
```

### 方案三：模拟器快速测试

#### 1. 启动模拟器
```bash
# 列出可用的AVD
emulator -list-avds

# 启动模拟器（替换为你的AVD名称）
emulator -avd Pixel_7_API_34 &
```

#### 2. 等待模拟器启动后安装
```bash
# 等待设备就绪
adb wait-for-device

# 安装应用
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🧪 功能测试清单

### 阶段1：基础功能测试
- [ ] 应用成功安装和启动
- [ ] 主界面正常显示
- [ ] API配置界面可用
- [ ] 动作管理界面可用

### 阶段2：输入法测试
- [ ] 在系统设置中启用输入法
- [ ] 切换到应用输入法
- [ ] 输入法界面正常显示
- [ ] 按钮动态生成正常

### 阶段3：核心功能测试
- [ ] API配置保存和读取
- [ ] 动作创建和编辑
- [ ] 文本捕获功能
- [ ] API请求和响应

### 阶段4：集成测试
- [ ] 完整的文本处理流程
- [ ] 错误处理机制
- [ ] 跨应用兼容性

## 🔧 测试工具和命令

### ADB调试命令
```bash
# 查看应用日志
adb logcat | grep "TranslateIME\|GenericLLMApiClient\|InputistApp"

# 查看输入法状态
adb shell dumpsys input_method

# 查看应用信息
adb shell dumpsys package com.inputist.universal

# 清除应用数据（重置测试）
adb shell pm clear com.inputist.universal
```

### 网络测试
```bash
# 测试网络连接
adb shell ping 8.8.8.8

# 检查网络权限
adb shell dumpsys package com.inputist.universal | grep permission
```

### 性能监控
```bash
# 查看内存使用
adb shell dumpsys meminfo com.inputist.universal

# 查看CPU使用
adb shell top | grep inputist
```

## 🐛 常见问题排查

### 问题1：构建失败
```bash
# 检查Gradle版本
./gradlew --version

# 清理缓存
./gradlew clean
rm -rf .gradle
```

### 问题2：安装失败
```bash
# 卸载旧版本
adb uninstall com.inputist.universal

# 重新安装
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 问题3：输入法无法启用
```bash
# 检查输入法服务
adb shell dumpsys input_method | grep inputist

# 手动启用（需要root权限）
adb shell ime enable com.inputist.universal/.ime.TranslateInputMethodService
```

### 问题4：API请求失败
```bash
# 检查网络连接
adb shell ping api.openai.com

# 查看网络日志
adb logcat | grep "OkHttp\|API"
```

## 📝 测试记录模板

### 测试环境
- 设备型号：
- Android版本：
- 测试时间：
- 构建版本：

### 测试结果
- [ ] 应用安装：✅/❌
- [ ] 界面显示：✅/❌
- [ ] 输入法启用：✅/❌
- [ ] API配置：✅/❌
- [ ] 文本处理：✅/❌

### 发现问题
1. 问题描述：
   - 复现步骤：
   - 预期结果：
   - 实际结果：

## 🚄 超快速测试（5分钟版本）

如果您只想快速验证项目可用性：

```bash
# 1. 一键构建（2分钟）
./build.sh

# 2. 快速安装（30秒）
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 3. 启动应用（10秒）
adb shell am start -n "com.inputist.universal/.ui.MainActivity"

# 4. 查看日志（持续监控）
adb logcat | grep "InputistApp"
```

### 最小可行测试
1. **应用启动** - 确认主界面显示
2. **API配置** - 填写测试API信息
3. **输入法切换** - 在设置中启用输入法
4. **基础功能** - 创建一个测试动作

## 📱 真机测试建议

### 推荐测试设备
- **小米手机**（HyperOS/MIUI）- 测试兼容性
- **华为手机**（HarmonyOS）- 测试权限管理
- **原生Android**（Pixel等）- 基准测试
- **三星手机**（One UI）- 界面适配

### 测试场景
1. **微信输入框** - 最常用场景
2. **浏览器地址栏** - URL处理
3. **邮件应用** - 长文本处理
4. **备忘录应用** - 多行文本

---

**使用建议**：先用模拟器快速验证基础功能，再用真机测试兼容性和用户体验。
