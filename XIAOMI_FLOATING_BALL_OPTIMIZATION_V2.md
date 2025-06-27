# 小米设备悬浮球优化方案 v2 - 多信号融合检测

## 问题分析

通过日志分析发现，在小米/红米Android 15设备上：
1. `InputMethodManager.isActive()` 方法不能准确反映软键盘的弹出/隐藏状态
2. 原始逻辑"第三方输入法 + isActive"导致悬浮球一直显示
3. 用户需求是"仅在软键盘弹出时显示悬浮球，隐藏时自动消失"

## 优化方案 v2

### 1. 多信号融合检测策略

#### 对于小米/红米设备：
- **连续性检测**：需要连续3次检测到相同状态才确认变化
- **状态稳定期**：状态变化后需要稳定3秒才最终确认
- **WindowInsets融合**：如果WindowInsets检测到键盘但逻辑认为没有，倾向于显示
- **第三方输入法过滤**：只对常见第三方输入法生效

#### 对于其他设备：
- **标准检测**：结合 `isActive` 和 WindowInsets 结果
- **双重确认**：任一信号检测到键盘即显示悬浮球

### 2. 关键参数调整

```java
// 连续检测阈值
private static final int XIAOMI_CONFIDENCE_THRESHOLD = 3; // 连续3次确认

// 状态稳定时间
private static final long XIAOMI_STATE_STABLE_DURATION = 3000; // 3秒稳定期

// 输入活动超时
private static final long INPUT_ACTIVITY_TIMEOUT = 5000; // 5秒（原10秒）

// 检查频率
小米设备: 1.5秒一次（更频繁）
其他设备: 2秒一次（标准）
```

### 3. 核心算法

```java
private boolean detectXiaomiKeyboardState(boolean isActive) {
    // 1. 必须是第三方输入法
    if (!isCommonThirdPartyIME(currentInputMethod)) return false;
    
    // 2. 统计连续状态
    if (isActive) {
        consecutiveActiveCount++;
        consecutiveInactiveCount = 0;
    } else {
        consecutiveInactiveCount++;
        consecutiveActiveCount = 0;
    }
    
    // 3. 基于连续性判断
    if (consecutiveActiveCount >= 3) newState = true;
    else if (consecutiveInactiveCount >= 3) newState = false;
    else newState = lastState; // 保持不变
    
    // 4. 状态变化需要稳定期确认
    if (newState != lastState && stableTime > 3000) {
        confirmStateChange();
    }
    
    // 5. WindowInsets融合
    if (windowInsetsDetected && !ourLogicState) {
        return true; // 优先显示
    }
    
    return finalState;
}
```

## 实现改进

### 1. KeyboardAwareFloatingBallService.java

**新增字段：**
- `consecutiveActiveCount` / `consecutiveInactiveCount` - 连续状态计数
- `lastXiaomiKeyboardState` / `lastXiaomiStateChangeTime` - 小米状态跟踪
- `XIAOMI_CONFIDENCE_THRESHOLD` - 连续检测阈值

**新增方法：**
- `isXiaomiDevice()` - 小米设备检测
- `detectXiaomiKeyboardState()` - 小米专用检测算法
- `resetXiaomiDetectionState()` - 重置检测状态
- `getEnhancedDebugInfo()` - 增强调试信息

### 2. MainActivity.java

**调试改进：**
- 调用增强调试信息显示
- 提供详细的算法说明
- 展示连续检测和状态稳定的过程

## 预期效果

### 1. 小米设备体验改善
- ✅ 避免"一直显示悬浮球"的问题
- ✅ 只在真正输入时显示
- ✅ 输入完成后自动隐藏
- ✅ 更好的响应性（1.5秒检查间隔）

### 2. 兼容性保持
- ✅ 其他设备保持原有逻辑
- ✅ Android 15特殊适配
- ✅ 向下兼容

### 3. 调试能力增强
- ✅ 详细的检测过程日志
- ✅ 连续状态计数显示
- ✅ 多信号融合状态展示

## 测试验证

### 1. 测试场景
1. **微信聊天**：点击输入框 → 悬浮球出现 → 发送消息 → 悬浮球消失
2. **浏览器搜索**：点击搜索框 → 悬浮球出现 → 完成搜索 → 悬浮球消失
3. **快速切换**：连续点击不同输入框，验证状态变化的准确性
4. **长时间输入**：长时间输入过程中悬浮球保持显示

### 2. 日志关键字
- `Xiaomi enhanced detection` - 小米检测过程
- `consecutive active/inactive` - 连续状态计数
- `WindowInsets override` - 信号融合决策
- `state confirmed change` - 状态变化确认

### 3. 调试入口
- MainActivity → "🔧 调试悬浮球" → 查看"增强检测说明"部分

## 后续优化方向

如果v2方案仍有问题，可考虑：

1. **更智能的阈值**：根据设备型号动态调整连续检测次数
2. **机器学习方法**：学习用户的输入模式，预测键盘状态
3. **系统事件监听**：监听更多系统级事件（焦点变化、窗口变化等）
4. **用户反馈机制**：允许用户手动校正检测结果，改进算法

---

**版本**: v2.0  
**更新时间**: 2024-12-28  
**适用设备**: 小米/红米 Android 15，通用Android设备  
**状态**: 待测试验证
