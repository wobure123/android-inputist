# 通用输入改写助手 V2.0 开发规划

## 📊 项目跟踪状态

### ✅ 已完成
- 项目初始化和规划
- Android项目基础结构搭建
- 依赖配置 (Gson, OkHttp, Encrypted SharedPreferences)
- 基础包结构创建
- 权限配置
- Action数据模型定义
- SettingsRepository实现
- 加密存储机制
- 数据序列化/反序列化
- GenericLLMApiClient实现
- OpenAI标准API集成
- TranslateInputMethodService基础框架
- 输入法注册与配置
- 文本捕获功能
- 动态按钮生成
- MainActivity (配置中心)
- ActionEditorActivity (动作编辑)
- GuideActivity (使用指南)
- ActionAdapter和GuideStepAdapter
- 基础UI布局和资源
- 应用图标和主题
- README文档
- 构建脚本
- 快速测试指南和脚本
- 详细测试用例集合

### 🚧 进行中
- 暂无

### ⏳ 待实现
- 错误处理和重试机制优化
- 网络请求性能优化
- 小米/HyperOS专项优化
- ANR预防机制
- 内存使用优化
- 电池优化白名单引导
- 单元测试用例
- 集成测试用例
- 真机兼容性测试
- 应用商店发布准备

### 🔄 需要优化
- 输入法界面美化
- 加载状态优化
- 错误提示完善
- 用户体验细节

---

## 🏗️ 实现阶段规划

### 阶段1: 项目基础设施 (Priority: High)
- [ ] Android项目初始化
- [ ] 依赖配置 (Gson, OkHttp, Encrypted SharedPreferences)
- [ ] 基础包结构创建
- [ ] 权限配置

### 阶段2: 数据模型与存储 (Priority: High)
- [ ] Action数据模型定义
- [ ] SettingsRepository实现
- [ ] 加密存储机制
- [ ] 数据序列化/反序列化

### 阶段3: 输入法服务核心 (Priority: High)
- [ ] TranslateInputMethodService基础框架
- [ ] 输入法注册与配置
- [ ] 文本捕获功能
- [ ] 动态按钮生成

### 阶段4: API客户端 (Priority: High)
- [ ] GenericLLMApiClient实现
- [ ] OpenAI标准API集成
- [ ] 错误处理和重试机制
- [ ] 网络请求优化

### 阶段5: 用户界面 (Priority: High)
- [ ] MainActivity (配置中心)
- [ ] ActionEditorActivity (动作编辑)
- [ ] RecyclerView适配器
- [ ] 用户引导界面

### 阶段6: 核心业务逻辑 (Priority: High)
- [ ] 动作执行流程
- [ ] 文本替换逻辑
- [ ] 系统指令处理
- [ ] 结果拼接格式化

### 阶段7: 用户体验优化 (Priority: Medium)
- [ ] 错误提示和反馈
- [ ] 加载状态指示
- [ ] 首次使用引导
- [ ] 界面美化

### 阶段8: 兼容性与性能 (Priority: Medium)
- [ ] 小米/HyperOS优化
- [ ] ANR预防
- [ ] 内存优化
- [ ] 电池优化白名单

### 阶段9: 测试与调试 (Priority: Medium)
- [ ] 单元测试
- [ ] 集成测试
- [ ] 真机测试
- [ ] 兼容性测试

### 阶段10: 发布准备 (Priority: Low)
- [ ] 应用图标和资源
- [ ] 应用签名配置
- [ ] 混淆配置
- [ ] 文档完善

---

## 📦 核心模块清单

### 数据层
- `model/Action.java` - 动作数据模型
- `repository/SettingsRepository.java` - 设置和数据存储
- `utils/CryptoUtils.java` - 加密工具类

### 网络层
- `api/GenericLLMApiClient.java` - API客户端
- `api/model/OpenAIRequest.java` - 请求模型
- `api/model/OpenAIResponse.java` - 响应模型

### 服务层
- `ime/TranslateInputMethodService.java` - 输入法服务
- `ime/InputMethodUtils.java` - 输入法工具类

### 界面层
- `ui/MainActivity.java` - 主界面/配置中心
- `ui/ActionEditorActivity.java` - 动作编辑界面
- `ui/adapter/ActionAdapter.java` - 动作列表适配器
- `ui/GuideActivity.java` - 使用引导界面

### 工具层
- `utils/JsonUtils.java` - JSON处理工具
- `utils/PermissionUtils.java` - 权限工具
- `utils/NetworkUtils.java` - 网络工具

---

## 🎯 技术选型确认

### 核心技术栈
- **语言**: Java (考虑后续升级到Kotlin)
- **最小SDK**: API 21 (Android 5.0)
- **目标SDK**: API 34 (Android 14)

### 主要依赖库
- **网络请求**: OkHttp3 + Retrofit2
- **JSON处理**: Gson
- **加密存储**: EncryptedSharedPreferences
- **UI组件**: Material Design Components
- **异步处理**: AsyncTask / Handler + Looper

### 权限需求
- `android.permission.INTERNET` - 网络请求
- `android.permission.BIND_INPUT_METHOD` - 输入法绑定
- `android.permission.WRITE_EXTERNAL_STORAGE` - 可选，用于导出配置

---

## 📱 界面设计要点

### 主界面 (MainActivity)
- 顶部: API配置区域 (URL, Key, Model)
- 中部: 动作列表 (RecyclerView)
- 底部: 添加动作按钮 + 使用指南按钮

### 动作编辑界面 (ActionEditorActivity)
- 动作名称输入框
- 系统指令多行输入框
- 预览/测试按钮
- 保存/取消按钮

### 输入法界面
- 动态生成的按钮网格
- 简洁的Material Design风格
- 适配不同屏幕尺寸

---

## 🔒 安全性考虑

### 数据加密
- API密钥使用EncryptedSharedPreferences存储
- 系统指令内容也需要加密存储
- 网络传输使用HTTPS

### 权限最小化
- 只申请必要的权限
- 运行时权限检查
- 用户隐私保护

---

## 📈 开发里程碑

1. **Day 1-2**: 项目基础设施搭建
2. **Day 3-4**: 数据模型和存储实现
3. **Day 5-7**: 输入法服务核心功能
4. **Day 8-9**: API客户端开发
5. **Day 10-12**: 用户界面实现
6. **Day 13-14**: 集成测试和优化
7. **Day 15**: 最终测试和发布准备

---

*最后更新: 2025年6月24日*
