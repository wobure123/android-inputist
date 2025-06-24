# GitHub Actions 自动构建指南

## 🚀 快速开始

### 1. 推送代码到GitHub
确保以下文件已上传到你的GitHub仓库：
- `app/` 文件夹（完整的Android项目）
- `build.gradle`
- `settings.gradle` 
- `gradle.properties`
- `local.properties`（可选，GitHub Actions会自动配置）
- `gradlew` 和 `gradlew.bat`
- `gradle/wrapper/` 文件夹
- `.github/workflows/build-apk.yml`（GitHub Actions配置文件）

### 2. 触发构建
有三种方式触发APK构建：

#### 方式一：推送代码（自动触发）
```bash
git add .
git commit -m "Add GitHub Actions build workflow"
git push origin main
```

#### 方式二：手动触发
1. 进入GitHub仓库页面
2. 点击 "Actions" 标签
3. 选择 "Build Android APK" 工作流
4. 点击 "Run workflow" 按钮

#### 方式三：创建Pull Request
向main分支创建Pull Request会自动触发构建

### 3. 下载APK
构建完成后：

1. **查看构建状态**：
   - 在GitHub仓库页面点击 "Actions" 标签
   - 查看最新的构建任务

2. **下载APK文件**：
   - 点击完成的构建任务
   - 在 "Artifacts" 部分下载：
     - `debug-apk`：调试版本APK
     - `release-apk`：发布版本APK（未签名）

3. **自动发布**（如果推送到main分支）：
   - 在 "Releases" 页面查看自动创建的发布版本
   - 直接下载APK文件

## 📱 安装到手机

1. **下载APK文件**到手机
2. **允许安装未知来源应用**：
   - 设置 → 安全 → 未知来源
   - 或在安装时按提示允许
3. **点击APK文件**开始安装

## 🔧 构建配置

### 环境要求
- Java 17
- Android SDK（GitHub Actions自动配置）
- Gradle 7.6

### 支持的架构
GitHub Actions使用x86_64架构，完美支持Android构建工具链。

### 构建输出
- **Debug APK**：`app-debug.apk`（包含调试信息，文件较大）
- **Release APK**：`app-release-unsigned.apk`（优化版本，需要签名才能发布到应用商店）

## 🚀 高级配置

### 自动签名（可选）
如果需要发布签名版本，可以在GitHub仓库设置中添加密钥：
1. 生成签名密钥
2. 在 Settings → Secrets 中添加签名信息
3. 修改构建流程以使用签名

### 构建优化
- 缓存Gradle依赖加速构建
- 并行构建支持
- 自动版本号管理

---

**注意**：首次构建可能需要5-10分钟，后续构建会因为缓存而更快。
