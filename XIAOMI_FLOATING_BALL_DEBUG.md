# 小米/红米 Android 15 悬浮球调试总结

## 当前状态
- ✅ 服务正常启动和运行
- ✅ 定时检查机制工作正常
- ✅ 权限配置正确
- ✅ 在应用内部能正常显示/隐藏悬浮球
- ❌ 在其他应用中检测键盘失效

## 核心问题
在小米/红米 Android 15 设备上，`InputMethodManager.isActive()` 行为异常：
- 在输入法应用内部：返回 `true`
- 在其他应用的输入框中：返回 `false`（即使键盘已弹出）

## 解决方案

### 1. 当前实施的改进
- **多重检测机制**：WindowInsets + ViewTreeObserver + 定时检查
- **小米设备特殊策略**：检测第三方输入法时直接显示悬浮球
- **防重复点击**：添加500ms防抖机制

### 2. 新的检测逻辑
```java
// 对于小米设备的特殊处理
if (isXiaomiDevice && isCommonThirdPartyIME(currentInputMethod)) {
    shouldShow = true;  // 直接显示悬浮球
}
```

### 3. 支持的第三方输入法
- 搜狗输入法 (sogou) ✅
- 百度输入法 (baidu)
- 讯飞输入法 (iflytek)
- QQ输入法 (qq)
- Google输入法 (gboard)
- SwiftKey (swiftkey)
- 以及各厂商默认输入法

## 预期效果

更新后的版本应该：
1. **在小米设备上**：只要使用第三方输入法就显示悬浮球
2. **在其他设备上**：使用标准的 `isActive()` 检测
3. **智能隐藏**：切换到 Inputist 输入法时自动隐藏

## 测试步骤

1. 重新构建并安装 APK
2. 启用悬浮球功能
3. 退出应用到桌面
4. 打开任意应用（如微信、浏览器）
5. 点击输入框，预期悬浮球应该立即显示

## 备用方案

如果问题仍然存在，可能需要：
1. **用户手动控制**：添加"始终显示悬浮球"选项
2. **应用白名单**：针对特定应用强制显示
3. **通知栏快捷开关**：提供快速切换功能

## 调试日志关键字

观察以下日志：
- `Xiaomi device - IME:` - 小米设备检测结果
- `isCommonThirdParty:` - 第三方输入法判断
- `Periodic check detected keyboard state change:` - 状态变化检测
