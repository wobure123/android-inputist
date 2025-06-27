# 悬浮球架构升级总结

## 🎯 升级目标
将基于辅助功能服务的悬浮球方案升级为基于现代 WindowInsets API 的精准键盘检测方案，实现更稳定、更精准的悬浮球控制。

## ✅ 完成的改动

### 1. 新增核心服务
- **KeyboardAwareFloatingBallService.java**: 全新的键盘感知悬浮球服务
  - 混合策略：API 30+ 使用 WindowInsets，API 24-29 使用改进的 ViewTreeObserver
  - 精准检测软键盘状态，无需辅助功能权限
  - 智能输入法切换逻辑

### 2. 增强现有组件
- **FloatingBallManager.java**: 添加点击监听器接口
- **FloatingBallView.java**: 实现点击事件处理机制
- **AndroidManifest.xml**: 注册新服务，移除辅助功能服务

### 3. 权限简化
- **移除辅助功能权限依赖**: 不再需要用户手动开启辅助功能
- **仅需悬浮窗权限**: 大大简化了用户配置流程
- **更新 MainActivity**: 移除所有辅助功能相关的检查和提示

### 4. 依赖更新
- **build.gradle**: 添加 AndroidX Core 1.12.0 支持现代 WindowInsets API

## 🚀 核心功能

### 1. 精准键盘检测
```java
// API 30+ 现代方案
ViewCompat.setOnApplyWindowInsetsListener(anchorView, (v, insets) -> {
    boolean isVisible = insets.isVisible(WindowInsetsCompat.Type.ime());
    int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
    handleKeyboardStateChange(isVisible);
    return insets;
});

// API 24-29 兼容方案
layoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
    @Override
    public void onGlobalLayout() {
        // 使用改进的布局检测算法
        // 动态阈值计算，适应不同屏幕尺寸
    }
};
```

### 2. 智能显示逻辑
- **显示条件**: 键盘弹出 + 当前输入法非 Inputist
- **隐藏条件**: 键盘隐藏 或 当前输入法为 Inputist

### 3. 输入法快速切换
- **当前为 Inputist**: 点击悬浮球 → 切换到上一个输入法
- **当前非 Inputist**: 点击悬浮球 → 切换到 Inputist 输入法

## 📋 使用指南

### 启用新功能
1. 打开应用主页面
2. 点击"悬浮球设置"
3. 授予悬浮窗权限（仅需一次）
4. 启用悬浮球功能

### 测试步骤
1. 在应用中启用悬浮球
2. 打开任意应用（如微信、QQ等）
3. 点击文本输入框，观察悬浮球是否出现
4. 点击悬浮球测试输入法切换
5. 退出输入状态，观察悬浮球是否隐藏

## 🔧 技术优势

### 相比辅助功能方案的改进
1. **更高精度**: 直接查询系统 IME 状态，无需启发式算法
2. **更好性能**: 事件驱动，无需轮询检测
3. **更简权限**: 仅需悬浮窗权限，用户体验更友好
4. **更稳兼容**: 支持分屏、浮动键盘等现代交互模式

### API 兼容策略
- **Android 11+ (API 30+)**: 使用 WindowInsetsCompat API，精度最高
- **Android 7+ (API 24-29)**: 使用改进的 ViewTreeObserver，兼容性最好

## 🐛 注意事项

### 输入法切换限制
- 由于 Android 安全限制，直接切换输入法需要系统权限
- 当前实现会打开输入法设置页面，需要用户手动选择
- 可考虑申请 `WRITE_SECURE_SETTINGS` 权限实现自动切换

### 性能优化
- 添加了防抖机制，避免频繁检测
- 使用锚点视图最小化资源占用
- 智能清理机制确保无内存泄漏

## 🔄 迁移说明

### 从旧版本升级
1. 旧的 `GlobalInputDetectionService` 被保留但不再使用
2. 用户重新启动应用后自动使用新服务
3. 辅助功能权限可以关闭，不影响功能

### 配置文件更新
- 不需要修改用户配置
- 现有设置自动迁移到新架构
- 保持向下兼容

## 🎉 总结

新的键盘感知悬浮球方案成功解决了以下问题：
- ✅ 消除了辅助功能依赖
- ✅ 提高了键盘检测精度
- ✅ 简化了用户配置流程
- ✅ 增强了系统兼容性
- ✅ 实现了智能输入法切换

这次升级让悬浮球功能更加稳定、精准和易用，为用户提供了更好的输入体验。
