# 悬浮球调试指南 - Android 15 红米设备

## 🐛 问题描述
在 Android 15 的红米手机上，悬浮球权限已开启，服务也启动了，但悬浮球始终不出现。

## 🔍 可能原因分析

### 1. Android 15 权限变更
Android 15 对悬浮窗权限有更严格的限制，特别是：
- 更严格的 `TYPE_APPLICATION_OVERLAY` 限制
- 新的隐私保护机制
- 厂商定制系统的额外限制

### 2. 红米/小米系统特殊限制
MIUI 系统可能有：
- 额外的悬浮窗管理限制
- 特殊的后台服务限制
- 与标准 Android 不同的权限检查机制

### 3. 服务启动问题
- `KeyboardAwareFloatingBallService` 可能未正确启动
- 锚点视图创建失败
- 悬浮球管理器初始化失败

## 🛠️ 调试步骤

### 第一步：检查日志
使用 ADB 命令查看详细日志：
```bash
# 清除日志
adb logcat -c

# 过滤相关日志
adb logcat -s KeyboardAwareFloatingBallService MainActivity FloatingBallManager FloatingBallView

# 或者查看所有日志
adb logcat | grep -E "(Floating|Keyboard|Overlay)"
```

### 第二步：使用应用内诊断
1. 打开应用主界面
2. 点击"悬浮球设置"
3. 点击"服务检查"按钮
4. 查看服务状态信息
5. 点击"测试悬浮球"按钮
6. 查看诊断信息对话框

### 第三步：手动权限检查
在红米设备上：
1. 设置 → 应用管理 → 输入法助手
2. 权限管理 → 悬浮窗权限（确认已开启）
3. 应用权限 → 显示在其他应用上层（确认已开启）
4. 后台弹出界面（如果有此选项，需要开启）

### 第四步：系统特殊设置
在 MIUI 系统中检查：
1. 设置 → 应用设置 → 应用管理
2. 找到"输入法助手"
3. 其他权限 → 后台弹出界面
4. 自启动管理（确保允许自启动）
5. 省电策略 → 无限制

## 🔧 修复措施

### 已实施的修复
1. **增强日志记录**: 添加详细的错误日志和状态信息
2. **重试机制**: 锚点视图创建失败时自动重试
3. **Android 15 适配**: 更新 targetSdk 到 35
4. **设备检测**: 特殊处理小米/红米设备
5. **诊断工具**: 添加服务状态查询和强制测试功能

### 代码层面修复
```java
// 1. 改进锚点视图创建
private void createAnchorView() {
    // 添加详细日志和重试机制
    // 特殊处理 Android 15 权限
}

// 2. 增强服务状态监控
public String getServiceStatus() {
    // 返回详细的服务状态信息
}

// 3. 强制测试功能
public void forceShowFloatingBall() {
    // 绕过正常逻辑强制显示悬浮球
}
```

## 📱 测试建议

### 立即测试
1. 重新编译安装更新后的 APK
2. 在应用中点击"悬浮球设置" → "服务检查"
3. 查看诊断信息，特别关注：
   - Anchor view 是否成功创建
   - Floating ball manager 是否初始化
   - 是否有特殊的错误信息

### 日志收集
请运行以下命令并分享日志：
```bash
# 启动日志收集
adb logcat -c
adb logcat -s KeyboardAwareFloatingBallService:V MainActivity:V FloatingBallManager:V > floating_ball_debug.log

# 然后在应用中：
# 1. 启用悬浮球功能
# 2. 点击"测试悬浮球"
# 3. 到其他应用测试输入框
# 4. 停止日志收集（Ctrl+C）
```

## 🚨 应急方案

如果新的键盘感知服务仍然有问题，可以临时回退到旧的方案：

### 方案A：使用旧的 FloatingBallService
临时修改 `toggleFloatingBall` 方法：
```java
Intent serviceIntent = new Intent(this, FloatingBallService.class); // 使用旧服务
```

### 方案B：添加厂商适配
为红米设备添加特殊的悬浮窗创建逻辑。

## 📞 后续支持
如果问题仍然存在，请提供：
1. 完整的 Logcat 日志
2. 设备具体型号和 MIUI 版本
3. 诊断信息截图
4. 是否在其他应用中见过类似悬浮球功能正常工作

这将帮助我们进一步定位和解决问题。
