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

# Gson specific rules

-keepattributes Signature

-keepattributes *Annotation*

-keepattributes EnclosingMethod

-keepattributes InnerClasses

-keep class com.google.gson.reflect.TypeToken { *; }

-keep class * extends com.google.gson.reflect.TypeToken



# Retrofit 2 rules

-keep class retrofit2.** { *; }

-keepattributes Exceptions

-dontwarn retrofit2.**

-keepclassmembers,allowshrinking,allowobfuscation interface * {

    @retrofit2.http.* <methods>;

}



# OkHttp 3 rules

-keep class okhttp3.** { *; }

-dontwarn okhttp3.**

-dontwarn okio.**



# Keep data classes and network interfaces

-keep class com.typeassist.app.data.** { *; }

-keep class com.typeassist.app.data.model.** { *; }

-keep class com.typeassist.app.data.network.** { *; }

-keepclassmembers class com.typeassist.app.data.model.** { *; }



# Fix for Kotlin Coroutines / Retrofit Reflection

-keep class kotlin.coroutines.Continuation { *; }

-keep class kotlin.Result { *; }

-keepnames class com.typeassist.app.data.network.** { *; }
