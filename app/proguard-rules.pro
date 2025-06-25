# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ========== 解释说明 ==========
# ProGuard规则说明：
# -keep：完全保留指定的类或成员，不进行混淆、优化或删除
# -keepclassmembers：只保留类的成员（方法、字段），类名可能仍会被混淆
# -keepattributes：保留指定的属性信息
# -dontwarn：忽略指定包的警告

# ========== 通用保护规则 ==========
# 保留所有重要的属性信息
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# ========== Gson 混淆规则 ==========
# 完全保留Gson相关类
-keep class com.google.gson.** { *; }
-keep class com.google.gson.reflect.TypeToken { *; }
-keep class * extends com.google.gson.reflect.TypeToken
-keep class com.google.gson.internal.** { *; }
-dontwarn com.google.gson.**

# ========== 应用数据模型保护 ==========
# 完全保护所有model类，防止字段和方法被混淆
-keep class com.inputassistant.universal.model.** {
    <fields>;
    <methods>;
    <init>(...);
}

# 特别保护关键的repository类
-keep class com.inputassistant.universal.repository.** {
    public <methods>;
    public <fields>;
}

# ========== OkHttp3 混淆规则 ==========
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**

# ========== Android 加密库规则 ==========
-keep class androidx.security.crypto.** { *; }
-dontwarn androidx.security.crypto.**

# ========== 保留反射和序列化 ==========
# 保留所有序列化相关
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 保留枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ========== 输入法服务保护 ==========
# 保护输入法服务类，防止Android系统无法找到
-keep class com.inputassistant.universal.ime.** {
    public <methods>;
    public <fields>;
}

# 保护Activity类的重要方法
-keep class com.inputassistant.universal.MainActivity {
    public <methods>;
    protected <methods>;
}

-keep class com.inputassistant.universal.ActionEditorActivity {
    public <methods>;
    protected <methods>;
}
