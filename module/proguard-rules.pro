# Keep Xposed hooks
-keep class de.robv.android.xposed.** { *; }
-keep class com.ghostos.pointblank.** { *; }

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep reflection targets
-keep class android.os.SystemProperties { *; }
-keep class com.android.server.SystemServer { *; }
-keep class com.android.internal.os.ZygoteInit { *; }

# Don't obfuscate Xposed entry points
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes Exceptions
