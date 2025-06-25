# ProGuard rules for Inputist
# 
# 注意：当前项目已禁用代码混淆 (minifyEnabled false)
# 此文件保留用于将来可能需要启用混淆的情况
#
# 如需启用混淆，请：
# 1. 在 app/build.gradle 中设置 minifyEnabled true
# 2. 取消注释下面的规则

# ========== 基础规则（暂时不需要）==========
# 如果将来启用混淆，取消下面的注释：

# 保留调试信息
#-keepattributes SourceFile,LineNumberTable

# 保留输入法服务（Android系统需要）
#-keep class com.inputassistant.universal.ime.** { *; }

# 保留数据模型（Gson序列化需要）  
#-keep class com.inputassistant.universal.model.** { *; }

# 保留Gson相关类
#-keep class com.google.gson.** { *; }
#-keep class com.google.gson.reflect.TypeToken { *; }

# 保留OkHttp3
#-keep class okhttp3.** { *; }
#-dontwarn okhttp3.**

# ========== 当前状态：无混淆 ==========
# 当前配置下，代码不会被混淆，因此：
# ✅ 调试信息完整保留
# ✅ 崩溃日志直接可读  
# ✅ 构建速度快
# ✅ 无需维护复杂规则
