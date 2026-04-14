package com.ghostos.pointblank

import android.util.Log
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers

/**
 * Zygote-level hooks that apply system-wide.
 * These run before any app or SystemServer is forked.
 */
object ZygoteHook {
    
    private const val TAG = "PointBlank/Zygote"
    private var prepared = false
    
    fun prepare(startupParam: IXposedHookZygoteInit.StartupParam) {
        if (prepared) return
        
        // Hook SystemProperties at the Zygote level
        // This ensures all forked processes inherit our hooks
        hookSystemProperties()
        
        // Hook ZygoteInit to block unnecessary preloading
        hookZygotePreload()
        
        prepared = true
    }
    
    /**
     * Hook SystemProperties.get() at Zygote level.
     * This is the PRIMARY hook for device spoofing.
     */
    private fun hookSystemProperties() {
        try {
            val systemPropertiesClass = XposedHelpers.findClass(
                "android.os.SystemProperties",
                null
            )
            
            // Hook the Java layer get() method
            XposedHelpers.findAndHookMethod(
                systemPropertiesClass,
                "get",
                String::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as String
                        val spoofedValue = PropertySpoofer.getSpoofedValue(key)
                        
                        if (spoofedValue != null) {
                            param.result = spoofedValue
                            if (PropertySpoofer.DEBUG) {
                                Log.d(TAG, "Spoofed $key = $spoofedValue")
                            }
                        }
                    }
                }
            )
            
            // Hook the native layer property_get to catch cached reads
            // Some apps read properties via JNI to bypass Java hooks
            XposedHelpers.findAndHookMethod(
                systemPropertiesClass,
                "native_get",
                String::class.java,
                String::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val key = param.args[0] as String
                        val spoofedValue = PropertySpoofer.getSpoofedValue(key)
                        
                        if (spoofedValue != null) {
                            param.result = spoofedValue
                        }
                    }
                }
            )
            
            Log.i(TAG, "✅ SystemProperties hooks installed")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to hook SystemProperties", e)
            XposedBridge.log("PointBlank: Property hook failed - ${e.message}")
        }
    }
    
    /**
     * Hook ZygoteInit.preload() to block OEM bloat from preloading.
     * This saves 200-300MB RAM on devices like Vivo/OPPO/Xiaomi.
     */
    private fun hookZygotePreload() {
        try {
            val zygoteInitClass = XposedHelpers.findClass(
                "com.android.internal.os.ZygoteInit",
                null
            )
            
            XposedHelpers.findAndHookMethod(
                zygoteInitClass,
                "preload",
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        // Modify the preloadClasses list to remove OEM bloat
                        try {
                            val preloadClassesField = zygoteInitClass
                                .getDeclaredField("PRELOADED_CLASSES")
                            preloadClassesField.isAccessible = true
                            
                            @Suppress("UNCHECKED_CAST")
                            val preloadList = preloadClassesField.get(null) as? MutableList<String>
                            
                            preloadList?.let {
                                val originalSize = it.size
                                val filtered = it.filterNot { className ->
                                    // Filter out OEM bloat classes
                                    className.contains("vivo") ||
                                    className.contains("oppo") ||
                                    className.contains("xiaomi") ||
                                    className.contains("bbk") ||
                                    className.contains("coloros") ||
                                    className.contains("funtouch") ||
                                    className.contains("miui") ||
                                    className.contains("oneplus")
                                }.toMutableList()
                                
                                preloadClassesField.set(null, filtered)
                                Log.i(TAG, "Blocked ${originalSize - filtered.size} OEM classes from preload")
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not modify preload list: ${e.message}")
                        }
                    }
                }
            )
            
            Log.i(TAG, "✅ Zygote preload hook installed")
        } catch (e: Exception) {
            Log.w(TAG, "Zygote preload hook failed (non-critical): ${e.message}")
        }
    }
}
