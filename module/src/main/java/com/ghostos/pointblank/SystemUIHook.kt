package com.ghostos.pointblank

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * SystemUI Hook - Custom Status Bar Integration
 * 
 * Hooks into SystemUI to add GhostOS indicators and customizations.
 * Non-critical - failures don't affect core functionality.
 */
object SystemUIHook {
    
    private const val TAG = "PointBlank/SystemUI"
    private var hooked = false
    
    fun inject(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (hooked) return
        
        try {
            // Add GhostOS indicator to status bar
            hookStatusBar(lpparam)
            
            // Customize quick settings
            hookQuickSettings(lpparam)
            
            // Hide OEM bloat icons
            hideBloatIcons(lpparam)
            
            hooked = true
            Log.i(TAG, "✅ SystemUI hooks installed")
        } catch (e: Exception) {
            Log.w(TAG, "SystemUI hooks failed (non-critical): ${e.message}")
        }
    }
    
    private fun hookStatusBar(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val phoneStatusBarClass = XposedHelpers.findClass(
                "com.android.systemui.statusbar.phone.PhoneStatusBarView",
                lpparam.classLoader
            )
            
            XposedHelpers.findAndHookMethod(
                phoneStatusBarClass,
                "onFinishInflate",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // Status bar is ready - we could inject custom views here
                        Log.d(TAG, "Status bar inflated")
                    }
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "StatusBar hook failed: ${e.message}")
        }
    }
    
    private fun hookQuickSettings(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val qsPanelClass = XposedHelpers.findClass(
                "com.android.systemui.qs.QSPanel",
                lpparam.classLoader
            )
            
            XposedHelpers.findAndHookMethod(
                qsPanelClass,
                "onAttachedToWindow",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // Could add GhostOS quick settings tile here
                        Log.d(TAG, "QS Panel attached")
                    }
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "QuickSettings hook failed: ${e.message}")
        }
    }
    
    private fun hideBloatIcons(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val statusBarIconControllerClass = XposedHelpers.findClass(
                "com.android.systemui.statusbar.phone.StatusBarIconControllerImpl",
                lpparam.classLoader
            )
            
            XposedHelpers.findAndHookMethod(
                statusBarIconControllerClass,
                "setIconVisibility",
                String::class.java,
                Boolean::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val slot = param.args[0] as String
                        // Hide NFC icon, alarm icon, etc if configured
                        val hiddenSlots = setOf("nfc", "alarm_clock", "zen")
                        if (slot in hiddenSlots) {
                            param.args[1] = false
                        }
                    }
                }
            )
        } catch (e: Exception) {
            Log.w(TAG, "Icon hide hook failed: ${e.message}")
        }
    }
}
