#!/bin/bash

# 通用输入改写助手 构建脚本
# 版本: V2.0
# 日期: 2025-06-24

set -e

echo "🚀 开始构建通用输入改写助手..."

# 检查环境
echo "📋 检查构建环境..."

if ! command -v java &> /dev/null; then
    echo "❌ Java未安装，请安装JDK 8或更高版本"
    exit 1
fi

if [ ! -f "gradlew" ]; then
    echo "❌ 未找到gradlew，请在项目根目录运行此脚本"
    exit 1
fi

echo "✅ 环境检查通过"

# 清理项目
echo "🧹 清理项目..."
./gradlew clean

# 构建Debug版本
echo "🔨 构建Debug APK..."
./gradlew assembleDebug

# 检查构建结果
if [ -f "app/build/outputs/apk/debug/app-debug.apk" ]; then
    echo "✅ Debug APK构建成功"
    echo "📍 位置: app/build/outputs/apk/debug/app-debug.apk"
    
    # 显示APK信息
    APK_SIZE=$(du -h "app/build/outputs/apk/debug/app-debug.apk" | cut -f1)
    echo "📦 APK大小: $APK_SIZE"
else
    echo "❌ Debug APK构建失败"
    exit 1
fi

# 可选：运行测试
read -p "🧪 是否运行单元测试? (y/N): " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "🧪 运行单元测试..."
    ./gradlew test
    echo "✅ 单元测试完成"
fi

# 可选：安装到连接的设备
if command -v adb &> /dev/null; then
    DEVICE_COUNT=$(adb devices | grep -c "device$" || true)
    if [ "$DEVICE_COUNT" -gt 0 ]; then
        read -p "📱 检测到Android设备，是否安装APK? (y/N): " -n 1 -r
        echo
        if [[ $REPLY =~ ^[Yy]$ ]]; then
            echo "📱 安装APK到设备..."
            adb install -r "app/build/outputs/apk/debug/app-debug.apk"
            echo "✅ 安装完成"
            
            # 启动应用
            read -p "🚀 是否启动应用? (y/N): " -n 1 -r
            echo
            if [[ $REPLY =~ ^[Yy]$ ]]; then
                adb shell am start -n "com.inputist.universal/.ui.MainActivity"
                echo "✅ 应用已启动"
            fi
        fi
    fi
fi

echo ""
echo "🎉 构建完成！"
echo ""
echo "📋 下一步操作："
echo "1. 安装APK到设备"
echo "2. 在系统设置中启用输入法"
echo "3. 配置API设置"
echo "4. 创建自定义功能"
echo "5. 开始使用！"
echo ""
echo "📚 详细使用说明请查看: README.md"
