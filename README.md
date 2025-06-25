# 通用输入改写助手

[![Build Android APK](https:### 4. 启用输### 5. 创建动作
1. 点击右下角的"+"按钮
2. 输入动作名称（如"翻译成英文"）
3. 输入系统指令（如"你是一个专业的翻译专家，请将用户输入的文本翻译成英文"）
4. 保存动作

### 6. 使用
1. 在任何应用的输入框中输入文本
2. 切换到"Inputist"输入法
3. 点击相应的动作按钮
4. 等待处理完成，文本将根据设置的模式更新输入法"按钮
2. 在系统设置中启用"Inputist"
3. 授予必要权限

### 5. 创建动作ub.com/wobure123/android-inputist/actions/workflows/build-apk.yml/badge.svg)](https://github.com/wobure123/android-inputist/actions/workflows/build-apk.yml)

一款Android输入法应用，集成大语言模型(LLM)进行文本智能处理。

## 📱 功能特性

### 核心功能
- **🎯 自定义动作流**: 支持用户创建个性化的文本处理动作
- **🤖 LLM集成**: 兼容OpenAI标准的API，支持多种大语言模型
- **⚡ 实时文本处理**: 在任何应用的输入框中一键处理文本
- **� 双模式处理**: 支持拼接模式和替换模式，满足不同使用需求
- **�🔒 安全存储**: API密钥使用Android EncryptedSharedPreferences加密存储
- **🌐 跨应用兼容**: 作为系统输入法，支持所有Android应用

### 使用场景
- **翻译**: 多语言文本翻译
- **润色**: 文案优化和改进
- **代码解释**: 代码分析和说明
- **角色扮演**: 基于角色的对话生成
- **自定义处理**: 根据系统指令进行个性化文本处理

## 🚀 快速开始

### 1. 安装应用
从 [Releases](https://github.com/wobure123/android-inputist/releases) 页面下载最新的APK文件。

### 2. 配置API
1. 打开应用
2. 填写API配置：
   - **API Base URL**: 如 `https://api.openai.com/v1`
   - **API Key**: 您的API密钥
   - **模型名称**: 如 `gpt-3.5-turbo`

### 3. 选择处理模式
- **拼接模式**: 保留原文并添加AI回答（默认）
- **替换模式**: 仅保留AI回答，替换原文
- 可在主界面切换处理模式

### 4. 启用输入法
1. 点击"设置输入法"按钮
2. 在系统设置中启用"通用输入改写助手"
3. 授予必要权限

### 4. 创建动作
1. 点击右下角的"+"按钮
2. 输入动作名称（如"翻译成英文"）
3. 输入系统指令（如"你是一个专业的翻译专家，请将用户输入的文本翻译成英文"）
4. 保存动作

### 5. 使用
1. 在任何应用的输入框中输入文本
2. 切换到"通用输入改写助手"输入法
3. 点击相应的动作按钮
4. 等待处理完成，文本将自动更新

## 🛠️ 技术架构

### 项目结构
```
app/
├── src/main/java/com/inputassistant/universal/
│   ├── MainActivity.java              # 主界面和配置中心
│   ├── ActionEditorActivity.java      # 动作编辑界面
│   ├── model/Action.java              # 动作数据模型
│   ├── repository/SettingsRepository.java  # 数据存储仓库
│   ├── api/GenericLLMApiClient.java   # 通用LLM API客户端
│   ├── adapter/ActionAdapter.java     # 动作列表适配器
│   └── ime/TranslateInputMethodService.java  # 输入法服务核心
└── src/main/res/
    ├── layout/                        # 布局文件
    ├── values/                        # 资源文件
    └── xml/method.xml                 # 输入法配置
```

### 核心技术
- **Android Input Method Framework**: 系统级输入法实现
- **EncryptedSharedPreferences**: 安全的本地数据存储
- **OkHttp3**: 网络请求处理
- **Gson**: JSON数据序列化
- **Material Design 3**: 现代化用户界面

## 🔧 开发构建

### 环境要求
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0+)
- JDK 8 or later

### 本地构建
```bash
git clone https://github.com/wobure123/android-inputist.git
cd android-inputist
./gradlew assembleDebug
```

### 自动构建
项目配置了GitHub Actions自动构建：
- 每次推送到main/master分支时自动构建
- 生成Debug和Release版本的APK
- 自动创建Release并上传APK文件

## 📋 API兼容性

支持任何兼容OpenAI Chat Completions API标准的服务：

### 官方OpenAI
```
Base URL: https://api.openai.com/v1
Models: gpt-3.5-turbo, gpt-4, gpt-4-turbo-preview
```

### 其他兼容服务
- Azure OpenAI Service
- Anthropic Claude (通过代理)
- 本地部署的Ollama
- 其他第三方API代理服务

## 🔐 安全说明

### 数据安全
- API密钥使用Android Keystore加密存储
- 不收集或上传用户的输入数据
- 所有网络请求直接发送到用户配置的API端点

### 权限说明
- **INTERNET**: 发送API请求
- **ACCESS_NETWORK_STATE**: 检查网络状态
- **BIND_INPUT_METHOD**: 注册为系统输入法
- **FOREGROUND_SERVICE**: 后台运行输入法服务

## 📝 使用示例

### 翻译动作
- **名称**: 翻译成英文
- **系统指令**: 你是一个专业的翻译专家，请将用户输入的文本准确流畅地翻译成英文。

### 润色动作
- **名称**: 文案润色
- **系统指令**: 你是一个资深文案专家，请帮助改进和润色用户的文本，使其更加专业、流畅和有感染力。

### 代码解释动作
- **名称**: 代码解释
- **系统指令**: 你是一个编程专家，请详细解释用户提供的代码的功能、逻辑和使用方法。

## 🤝 贡献指南

欢迎提交Issue和Pull Request！

### 开发流程
1. Fork项目
2. 创建功能分支
3. 提交更改
4. 推送到分支
5. 创建Pull Request

## 📄 许可证

[MIT License](LICENSE)

## 🔗 相关链接

- [OpenAI API文档](https://platform.openai.com/docs/api-reference)
- [Android输入法开发指南](https://developer.android.com/guide/topics/text/creating-input-method)
- [Material Design 3](https://m3.material.io/)

---

## 📊 状态追踪

### ✅ 已完成功能
- [x] 基础Android项目结构
- [x] 输入法服务实现
- [x] API配置和安全存储
- [x] 动作管理(CRUD)
- [x] 动态UI生成
- [x] LLM API集成
- [x] 文本捕获和替换
- [x] GitHub Actions自动构建
- [x] 快捷操作栏（删除、空格、换行、切换）
- [x] 一键输入法切换器
- [x] 双模式文本处理（拼接/替换）
- [x] 编译错误修复（showToast方法补充）
- [x] CI/CD签名配置优化

### 🔄 待优化功能
- [ ] 错误处理优化
- [ ] 用户体验改进
- [ ] 性能优化
- [ ] 多语言支持
- [ ] 更多预设动作模板

### 📈 更新日志

**v2.2.0 (2025-06-25)**
- ✨ 新增双模式文本处理功能
  - 🔄 拼接模式：保留原文 + 分割线 + AI回答
  - 🔄 替换模式：仅保留AI回答，替换原文
- 🎛️ 主界面新增处理模式设置开关
- 💾 文本处理模式设置持久化保存
- 🔔 处理完成提示显示当前使用模式
- 🐛 修复编译错误：补充MainActivity.showToast方法
- 🔧 优化CI/CD签名配置，解决Release构建问题

**v2.1.0 (2025-06-25)**
- 🎮 新增快捷操作栏
  - ⌫ 删除按钮：删除光标前字符
  - ␣ 空格按钮：插入空格
  - ↵ 换行按钮：插入换行符
  - 🔄 切换按钮：一键调出输入法选择器
- 🎨 优化输入法界面布局
- 📱 改进用户体验流程
- 🔧 修复输入法切换问题

**v2.0.0 (2025-06-24)**
- 🚀 初始版本发布
- ✨ 完整的自定义动作流系统
- 🔧 修复AndroidX兼容性问题
- 📦 优化构建配置和依赖管理
- 🎯 实现基础输入法功能

如有问题或建议，请提交Issue或联系开发者。
