#!/bin/bash

# 通用输入改写助手 - 快速测试脚本
# 版本: V2.0
# 日期: 2025-06-24

set -e

echo "🧪 通用输入改写助手 - 快速测试脚本"
echo "======================================"

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 错误处理函数
error_exit() {
    echo -e "${RED}❌ 错误: $1${NC}" >&2
    exit 1
}

# 成功提示函数
success_msg() {
    echo -e "${GREEN}✅ $1${NC}"
}

# 警告提示函数
warning_msg() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

# 信息提示函数
info_msg() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

# 检查是否在项目根目录
if [ ! -f "gradlew" ]; then
    error_exit "请在项目根目录运行此脚本（找不到gradlew文件）"
fi

# 1. 环境检查
echo ""
echo "📋 第一步：环境检查"
echo "----------------"

# 检查Java
if command -v java &> /dev/null; then
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    success_msg "Java版本: $JAVA_VERSION"
else
    error_exit "未安装Java，请安装JDK 8或更高版本"
fi

# 检查ADB
if command -v adb &> /dev/null; then
    success_msg "ADB工具已安装"
else
    warning_msg "未找到ADB工具，将无法自动安装到设备"
fi

# 检查连接的设备
if command -v adb &> /dev/null; then
    DEVICE_COUNT=$(adb devices | grep -c "device$" || true)
    if [ "$DEVICE_COUNT" -gt 0 ]; then
        success_msg "检测到 $DEVICE_COUNT 个Android设备"
        echo "设备列表:"
        adb devices
    else
        warning_msg "未检测到Android设备，请连接设备或启动模拟器"
    fi
fi

# 2. 快速构建
echo ""
echo "🔨 第二步：快速构建"
echo "----------------"

info_msg "开始清理项目..."
./gradlew clean > /dev/null 2>&1

info_msg "开始构建Debug APK..."
./gradlew assembleDebug

# 检查APK是否生成
APK_PATH="app/build/outputs/apk/debug/app-debug.apk"
if [ -f "$APK_PATH" ]; then
    APK_SIZE=$(du -h "$APK_PATH" | cut -f1)
    success_msg "APK构建成功！大小: $APK_SIZE"
    info_msg "APK位置: $APK_PATH"
else
    error_exit "APK构建失败"
fi

# 3. 安装测试
if command -v adb &> /dev/null && [ "$DEVICE_COUNT" -gt 0 ]; then
    echo ""
    echo "📱 第三步：安装测试"
    echo "----------------"
    
    read -p "是否安装到设备进行测试? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        info_msg "正在安装APK..."
        
        # 先尝试卸载旧版本
        adb uninstall com.inputist.universal &> /dev/null || true
        
        # 安装新版本
        if adb install "$APK_PATH"; then
            success_msg "应用安装成功！"
            
            # 启动应用
            read -p "是否启动应用? (y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                info_msg "正在启动应用..."
                adb shell am start -n "com.inputist.universal/.ui.MainActivity"
                success_msg "应用已启动！"
                
                # 开始日志监控
                echo ""
                info_msg "开始日志监控（按Ctrl+C停止）..."
                echo "日志过滤: InputistApp, TranslateIME, GenericLLMApiClient"
                echo "----------------------------------------"
                adb logcat | grep -E "(InputistApp|TranslateIME|GenericLLMApiClient)"
            fi
        else
            error_exit "应用安装失败"
        fi
    fi
fi

# 4. 提供测试指南
echo ""
echo "📚 第四步：测试指南"
echo "----------------"

echo ""
echo "🎯 快速测试步骤："
echo "1. 打开应用，配置API设置"
echo "2. 进入系统设置 → 语言和输入法 → 虚拟键盘"
echo "3. 启用'通用输入改写助手'"
echo "4. 在任意输入框中切换输入法"
echo "5. 测试文本处理功能"

echo ""
echo "🔧 有用的调试命令："
echo "查看应用日志: adb logcat | grep 'InputistApp'"
echo "查看输入法状态: adb shell dumpsys input_method"
echo "清除应用数据: adb shell pm clear com.inputist.universal"
echo "查看内存使用: adb shell dumpsys meminfo com.inputist.universal"

echo ""
echo "📖 详细测试指南请查看: TESTING_GUIDE.md"

echo ""
success_msg "快速测试准备完成！"
echo ""

# 5. 可选的自动化测试
if command -v adb &> /dev/null && [ "$DEVICE_COUNT" -gt 0 ]; then
    echo "🤖 第五步：自动化基础检查"
    echo "----------------------"
    
    read -p "是否运行自动化基础检查? (y/N): " -n 1 -r
    echo
    if [[ $REPLY =~ ^[Yy]$ ]]; then
        info_msg "检查应用是否已安装..."
        if adb shell pm list packages | grep -q "com.inputist.universal"; then
            success_msg "应用已正确安装"
        else
            warning_msg "应用未安装或安装异常"
        fi
        
        info_msg "检查应用权限..."
        PERMISSIONS=$(adb shell dumpsys package com.inputist.universal | grep -A 20 "requested permissions:" | grep "android.permission" || true)
        if [ -n "$PERMISSIONS" ]; then
            success_msg "权限配置正常"
            echo "$PERMISSIONS"
        else
            warning_msg "权限配置可能有问题"
        fi
        
        info_msg "检查网络连接..."
        if adb shell ping -c 1 8.8.8.8 &> /dev/null; then
            success_msg "网络连接正常"
        else
            warning_msg "网络连接异常"
        fi
        
        success_msg "自动化检查完成！"
    fi
fi

echo ""
echo "🎉 测试脚本执行完成！"
echo ""
echo "下一步："
echo "1. 手动测试应用功能"
echo "2. 配置您的API密钥"
echo "3. 创建自定义动作"
echo "4. 在实际场景中使用"
echo ""
echo "如遇问题，请查看 TESTING_GUIDE.md 或检查日志输出"
