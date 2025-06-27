# 签名配置说明

## 问题背景

在 GitHub Actions CI/CD 环境中构建 Android APK 时，每次构建都会生成新的 debug keystore，导致应用签名不一致。这会引发以下问题：

- 无法覆盖安装已安装的应用
- 出现 `INSTALL_FAILED_UPDATE_INCOMPATIBLE` 错误
- 用户数据丢失（需要卸载重装）

## 解决方案

### 1. 使用固定的 Debug Keystore

项目中已包含一个固定的 debug keystore 文件：`keystore/debug.keystore`

**特性：**
- 密码：`android`
- 别名：`androiddebugkey`
- 有效期：10,000 天
- 在所有构建环境中保持一致

### 2. 签名配置

```gradle
signingConfigs {
    debug {
        storeFile file('../keystore/debug.keystore')
        storePassword 'android'
        keyAlias 'androiddebugkey'
        keyPassword 'android'
    }
    release {
        // 开发阶段使用相同的签名
        storeFile file('../keystore/debug.keystore')
        storePassword 'android'
        keyAlias 'androiddebugkey'
        keyPassword 'android'
    }
}
```

## 安装说明

### 首次安装
直接安装 APK 文件即可。

### 更新安装
由于使用固定签名，可以直接覆盖安装，无需卸载。

### 如果仍然出现签名错误
如果之前安装的是不同签名的版本，需要先卸载：

```bash
# 卸载现有版本
adb uninstall com.inputassistant.universal.debug

# 安装新版本
adb install app-debug.apk
```

## 安全说明

⚠️ **重要提示：**
- 此 keystore 仅用于开发和测试
- **不要**在生产环境中使用此签名
- 发布到应用商店前需要使用正式的发布签名

## 生产环境部署

发布正式版本时，需要：

1. 生成正式的发布 keystore
2. 更新 `release` 签名配置
3. 妥善保管 keystore 文件和密码
4. 使用 GitHub Secrets 存储敏感信息

```gradle
release {
    signingConfig signingConfigs.release
    // 其他配置...
}
```
