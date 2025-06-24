# 通用输入改写助手 V2.0

![App Logo](app/src/main/res/drawable/ic_launcher_foreground.xml)

## 📖 项目简介

通用输入改写助手是一款创新的Android输入法应用，旨在为用户提供高度可定制的文本处理功能。通过集成LLM API，用户可以在任何应用的输入框中一键实现文本翻译、润色、总结等功能。

### 🌟 核心特性

- **🔧 自定义功能按钮**: 用户可自由创建、编辑、删除功能按钮
- **🤖 自定义System Prompt**: 每个按钮绑定独特的AI指令
- **🌐 通用API集成**: 支持任何兼容OpenAI标准的LLM API
- **🔒 安全存储**: 采用Android EncryptedSharedPreferences加密存储
- **⚡ 高性能**: 异步网络请求，避免ANR问题
- **📱 跨应用兼容**: 支持任何有输入框的Android应用

## 🏗️ 架构设计

### 技术栈
- **开发语言**: Java
- **最小SDK**: API 21 (Android 5.0)
- **目标SDK**: API 34 (Android 14)
- **核心库**: OkHttp3, Gson, Material Design Components

### 核心模块

```
com.inputist.universal/
├── model/                    # 数据模型
│   ├── Action.java          # 动作数据模型
│   └── ApiConfig.java       # API配置模型
├── repository/               # 数据仓库
│   └── SettingsRepository.java
├── api/                     # 网络API
│   ├── GenericLLMApiClient.java
│   └── model/               # API数据模型
├── ime/                     # 输入法服务
│   └── TranslateInputMethodService.java
├── ui/                      # 用户界面
│   ├── MainActivity.java   # 主界面/配置中心
│   ├── ActionEditorActivity.java # 动作编辑
│   ├── GuideActivity.java   # 使用指南
│   └── adapter/             # 列表适配器
└── utils/                   # 工具类
```

## 🚀 快速开始

### 环境要求
- Android Studio Flamingo 或更高版本
- JDK 8 或更高版本
- Android SDK 34

### 编译步骤

1. **克隆项目**
   ```bash
   git clone https://github.com/your-repo/android-inputist.git
   cd android-inputist
   ```

2. **打开项目**
   使用Android Studio打开项目目录

3. **同步Gradle**
   点击"Sync Now"同步Gradle依赖

4. **构建APK**
   ```bash
   ./gradlew assembleDebug
   ```

### 安装配置

1. **安装APK**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

2. **启用输入法**
   - 进入系统设置 → 语言和输入法 → 虚拟键盘
   - 启用"通用输入改写助手"

3. **配置API**
   - 打开应用，在配置中心填写API信息
   - 测试API连接确保配置正确

4. **创建功能**
   - 点击"+"按钮添加自定义功能
   - 设置功能名称和对应的System Prompt

## 📱 使用方法

### 基本使用流程

1. **切换输入法**
   - 在任意输入框中长按空格键
   - 选择"通用输入改写助手"

2. **处理文本**
   - 在输入框中输入或粘贴文本
   - 点击对应的功能按钮
   - 等待AI处理并自动替换结果

3. **查看结果**
   - 处理结果会以"原文 + --- + 处理结果"的格式显示
   - 可以继续编辑或直接发送

### 功能定制

#### 创建翻译功能
- **功能名称**: 翻译成英文
- **System Prompt**: "You are an expert translator. Please translate the user's text into fluent, natural English."

#### 创建润色功能
- **功能名称**: 文本润色
- **System Prompt**: "你是一个专业的文本编辑师。请对用户提供的文本进行润色，使其更加通顺、优雅、专业。"

## 🔧 开发指南

### 添加新功能

1. **扩展API客户端**
   ```java
   // 在GenericLLMApiClient中添加新的请求方法
   public void executeCustomRequest(...)
   ```

2. **更新数据模型**
   ```java
   // 在Action类中添加新的属性
   private String customField;
   ```

3. **修改UI界面**
   ```xml
   <!-- 在相应的布局文件中添加新控件 -->
   ```

### 测试指南

#### 单元测试
```bash
./gradlew test
```

#### 集成测试
```bash
./gradlew connectedAndroidTest
```

#### 手动测试清单
- [ ] API配置保存和加载
- [ ] 功能按钮创建和删除
- [ ] 输入法切换和文本捕获
- [ ] 网络请求和错误处理
- [ ] 不同设备兼容性

## 🐛 问题排查

### 常见问题

**Q: 输入法无法启用？**
A: 检查系统设置中是否正确启用了输入法，部分设备需要额外的权限授予。

**Q: API请求失败？**
A: 
1. 检查网络连接
2. 验证API配置是否正确
3. 确认API密钥有效
4. 查看LogCat中的详细错误信息

**Q: 在某些应用中无法工作？**
A: 某些应用可能限制了第三方输入法的功能，这是Android系统的安全机制。

### 调试方法

1. **查看日志**
   ```bash
   adb logcat | grep "TranslateIME\|GenericLLMApiClient"
   ```

2. **检查网络**
   ```bash
   adb shell ping 8.8.8.8
   ```

3. **验证权限**
   ```bash
   adb shell dumpsys input_method
   ```

## 🤝 贡献指南

### 开发规范

1. **代码风格**: 遵循Google Java Style Guide
2. **提交信息**: 使用Conventional Commits格式
3. **分支策略**: GitFlow工作流

### 提交流程

1. Fork本仓库
2. 创建功能分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送分支 (`git push origin feature/AmazingFeature`)
5. 创建Pull Request

## 📄 许可证

本项目采用MIT许可证 - 详见 [LICENSE](LICENSE) 文件

## 📞 联系我们

- **项目主页**: https://github.com/your-repo/android-inputist
- **问题反馈**: https://github.com/your-repo/android-inputist/issues
- **邮箱**: your-email@example.com

## 🙏 致谢

感谢以下开源项目的支持：
- [OkHttp](https://square.github.io/okhttp/) - HTTP客户端
- [Gson](https://github.com/google/gson) - JSON序列化
- [Material Design Components](https://material.io/develop/android) - UI组件库

---

**开发时间**: 2025年6月24日  
**版本**: V2.0  
**状态**: 活跃开发中 🚀
