# ProGuard rules for Inputist v2.6.0
# 
# 代码混淆和优化规则
# 在 Release 版本中启用以提升性能和减小APK体积

# ========== 基础保留规则 ==========

# 保留调试信息（用于崩溃日志分析）
-keepattributes SourceFile,LineNumberTable

# 保留注解信息
-keepattributes *Annotation*

# 保留泛型信息
-keepattributes Signature

# ========== Android 框架相关 ==========

# 保留 Activity, Service, Receiver 等组件
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# 保留输入法服务（系统级服务，必须保留）
-keep class com.inputassistant.universal.ime.** { *; }

# 保留悬浮球服务（前台服务，关键功能）
-keep class com.inputassistant.universal.floating.** {
    public <methods>;
    public <fields>;
}

# ========== 数据模型和序列化 ==========

# 保留数据模型（Gson 序列化需要）
-keep class com.inputassistant.universal.model.** { *; }

# 保留 SettingsRepository 相关（加密存储）
-keep class com.inputassistant.universal.repository.** {
    public <methods>;
}

# ========== 第三方库规则 ==========

# Gson 混淆规则
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# OkHttp3 规则
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# AndroidX 安全库
-keep class androidx.security.crypto.** { *; }

# ========== 优化规则 ==========

# 移除日志代码（Release 版本）
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# 移除 BuildConfig.DEBUG 相关代码
-assumenosideeffects class com.inputassistant.universal.BuildConfig {
    public static final boolean DEBUG return false;
    public static final boolean DEBUG_LOGGING return false;
}

# ========== 警告忽略 ==========

# 忽略不重要的警告
-dontwarn java.lang.invoke.*
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
