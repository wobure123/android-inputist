# 版本更新记录 - v2.3.0

## 📋 版本信息更新

### 构建版本更新
- **Version Code**: `3` → `4`
- **Version Name**: `"2.2.0"` → `"2.3.0"`
- **更新理由**: 新增智能悬浮球功能，属于次要版本更新

### 语义化版本说明
- **2.3.0** = 主版本.次版本.修订版本
  - 主版本 (2): 核心架构版本
  - 次版本 (3): 新增悬浮球功能 (从2升级到3)
  - 修订版本 (0): 新功能的初始版本

## 🚀 新版本特性 v2.3.0

### 🎈 智能悬浮球功能
- ⚡ **全局输入检测** - 任何输入法激活输入框时都会显示悬浮球
- 🚀 一键快速切换到输入法助手
- 🎨 支持拖拽移动和磁性吸附到屏幕边缘
- 🎛️ 点击悬浮球显示快捷菜单
- 🔒 智能权限管理和用户引导（悬浮窗 + 辅助功能）
- 💾 位置记忆功能，记住用户习惯位置
- 🌐 **不依赖Inputist输入法** - 与任何输入法配合使用

### 🛠️ 技术架构优化
- 🎯 **全局监听架构** - 基于辅助功能服务的全局输入检测
- 🔧 支持悬浮窗权限 + 辅助功能权限的双重验证
- ⚡ 极低资源消耗，按需启动
- 🏗️ 模块化设计，易于扩展和维护
- 🌐 **输入法无关性** - 不依赖特定输入法，与任何输入法兼容

### 🎨 新增UI组件
- 🎈 FloatingBallView - 悬浮球视图
- 📋 FloatingMenuView - 快捷菜单
- 🎛️ FloatingBallManager - 悬浮球管理器
- 🛠️ PermissionHelper - 权限管理工具
- 🔧 InputMethodHelper - 输入法切换工具
- 🌐 GlobalInputDetectionService - 全局输入检测服务
- 🔐 AccessibilityHelper - 辅助功能权限管理工具

### 📱 界面优化
- 🏷️ 主界面标题自动显示版本号（如：通用输入改写助手 v2.3.0）
- 🔍 便于用户识别当前应用版本
- 📊 增强用户体验和问题排查便利性

## 📦 构建配置变更

### app/build.gradle
```groovy
// 之前版本
versionCode 3
versionName "2.2.0"

// 新版本
versionCode 4
versionName "2.3.0"  // 新增智能悬浮球功能
```

### 权限更新
```xml
<!-- 新增悬浮窗权限 -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
```

### 服务注册
```xml
<!-- 悬浮球服务 -->
<service
    android:name=".floating.FloatingBallService"
    android:exported="false" />
    
<!-- 全局输入检测服务（辅助功能） -->
<service
    android:name=".service.GlobalInputDetectionService"
    android:exported="true"
    android:permission="android.permission.BIND_ACCESSIBILITY_SERVICE">
    <intent-filter>
        <action android:name="android.accessibilityservice.AccessibilityService" />
    </intent-filter>
    <meta-data
        android:name="android.accessibilityservice"
        android:resource="@xml/accessibility_service_config" />
</service>
```

## 🔄 向后兼容性

### ✅ 完全兼容
- 所有现有功能保持不变
- 原有API配置继续有效
- 用户数据和设置不受影响
- 现有动作和文本处理模式正常工作

### 🆕 可选功能
- 悬浮球功能为可选启用
- 不影响不使用悬浮球的用户
- 权限申请仅在用户主动使用时进行

## 🎯 升级建议

### 用户升级步骤
1. **安装新版本** - 覆盖安装或全新安装
2. **体验悬浮球** - 点击主界面"🎈 悬浮球设置"
3. **授予权限** - 根据引导开启悬浮窗权限和辅助功能权限
4. **启用悬浮球** - 在设置对话框中启用悬浮球功能
5. **享受便捷** - 在任意应用中体验智能悬浮球（无需切换到Inputist输入法）

### 开发者注意事项
- 新增了多个模块，注意代码审查
- 权限申请逻辑已完善，无需额外处理
- 悬浮球功能完全模块化，易于调试和维护

## 🧪 测试建议

### 功能测试
- [ ] **权限配置测试** - 悬浮窗权限和辅助功能权限申请流程
- [ ] **辅助功能服务** - 在设置→辅助功能→已下载应用中启用Inputist
- [ ] **软键盘检测** - 在任何应用中点击输入框，软键盘弹出时悬浮球应显示
- [ ] **输入法状态检测** - 切换到Inputist输入法后悬浮球应自动隐藏
- [ ] **悬浮球交互** - 点击悬浮球显示输入法切换菜单
- [ ] **权限检测机制** - 返回应用后的权限状态提示  
- [ ] **测试功能** - 使用主界面的"测试悬浮球"按钮
- [ ] 拖拽和磁性吸附
- [ ] 与现有功能的兼容性
- [ ] 悬浮球启用/禁用开关

### 简化版逻辑验证 (v2.3.0)
- [ ] **软键盘弹出检测** - 任何输入法弹出时悬浮球显示
- [ ] **Inputist输入法检测** - 切换到Inputist后悬浮球自动隐藏
- [ ] **软键盘隐藏检测** - 软键盘隐藏时悬浮球自动隐藏
- [ ] **快速切换功能** - 点击悬浮球→选择Inputist→成功切换→悬浮球消失

### 调试信息
- 🔍 GlobalInputDetectionService 提供基本的软键盘状态检测日志
- 🔍 FloatingBallService 提供悬浮球显示/隐藏操作日志
- 🔍 使用 `adb logcat -s GlobalInputDetectionService FloatingBallService` 查看关键日志
- 🔍 简化版去除了多余的调试信息，专注核心功能

### 设备兼容性测试
- [ ] Android 7.0+ (API 24+)
- [ ] 不同屏幕尺寸和分辨率
- [ ] 不同厂商的系统定制
- [ ] 权限管理策略差异

## 🎊 发布准备

版本v2.3.0已准备就绪，包含了激动人心的智能悬浮球功能！这个版本将大大提升用户体验，让AI文本处理变得更加便捷和智能。

**下一步操作**：
1. 进行全面测试
2. 准备发布说明
3. 更新应用商店描述
4. 发布新版本！🚀
