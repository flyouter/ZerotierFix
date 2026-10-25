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

# ZeroTier JNI：externals/core/java/jni/ZT_jnicache.cpp 通过 FindClass/GetMethodID
# 按类名与方法名查找这些类，混淆或裁剪会导致节点初始化直接失败。
-keep class com.zerotier.sdk.** { *; }

# 兜底：保留所有 native 方法名（类名已由上一条保留）
-keepclasseswithmembernames class * { native <methods>; }
