#!/bin/bash

echo "🚀 准备上传GitHub Actions构建文件（已修复AndroidX配置）..."

# 添加所有文件
git add .

# 提交更改
git commit -m "修复所有XML资源文件中的命名空间声明错误

- 修正所有布局和菜单文件中的 xmlns:app 命名空间声明
- 从错误的 'http://schemas.android.com/apk/res/android' 修正为正确的 'http://schemas.android.com/apk/res-auto'
- 修复了 CardView、Material Design 组件属性无法识别的问题
- 影响文件：activity_main.xml, activity_guide.xml, activity_action_editor.xml, main_menu.xml, action_editor_menu.xml
- 确保 AAPT2 能正确解析所有资源属性和命名空间
- 解决了 Android resource linking failed 错误"

echo "✅ 文件已提交到本地仓库"
echo ""
echo "📤 现在推送到GitHub："
echo "git push origin main"
echo ""
echo "🔗 推送后，访问你的GitHub仓库："
echo "   1. 点击 'Actions' 标签查看构建进度"
echo "   2. 构建完成后在 'Artifacts' 下载APK"
echo "   3. 或在 'Releases' 页面下载发布版本（APK直接附加）"
echo ""
echo "🛠️  已修复的关键问题："
echo "   - gradle.properties 被 .gitignore 排除（已修复）"
echo "   - AndroidX 配置现在会正确推送到GitHub"
echo "   - GitHub Actions版本兼容性问题"
