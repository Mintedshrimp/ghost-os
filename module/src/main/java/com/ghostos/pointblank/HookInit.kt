package com.ghostos.pointblank

import android.util.Log
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * Point Blank - SystemServer Injection Entry Point
 * 
 * This is the first code that runs when LSPosed loads our module.
 * We hook Zygote to prepare property interception, then target
 * SystemServer specifically to spawn the GhostOS launcher process.
 */
class HookInit : IXposedHookZygoteInit, IXposedHookLoadPackage {
    
    companion object {
        private const val TAG = "PointBlank"
        private var zygotePrepared = false
    }
    
    /**
     * Called during Zygote initialization - BEFORE any apps or SystemServer fork.
     * This is where we set up hooks that need to be ready before processes spawn.
     */
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam) {
        Log.i(TAG, "🧬 Point Blank initializing in Zygote...")
        
        try {
            // Prepare property spoofing hooks at Zygote level
            ZygoteHook.prepare(startupParam)
            zygotePrepared = true
            Log.i(TAG, "✅ Zygote hooks prepared successfully")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Failed to prepare Zygote hooks", e)
            XposedBridge.log("Point Blank: Zygote preparation failed - ${e.message}")
        }
    }
    
    /**
     * Called when ANY package is loaded.
     * We specifically target the "android" package (SystemServer) to inject our spawner.
     */
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // CRITICAL: Only target SystemServer process
        // packageName == "android" AND processName == "android"
        // This ensures we're in the actual SystemServer, not an app with "android" in its name
        if (lpparam.packageName == "android" && lpparam.processName == "android") {
            Log.i(TAG, "🎯 SystemServer detected! Injecting GhostSpawner...")
            
            try {
                SystemServerHook.inject(lpparam)
                Log.i(TAG, "✅ SystemServer injection complete")
                XposedBridge.log("Point Blank: SystemServer successfully injected")
            } catch (e: Exception) {
                Log.e(TAG, "❌ SystemServer injection failed", e)
                XposedBridge.log("Point Blank: Injection failed - ${e.message}")
            }
        }
        
        // Optional: Hook SystemUI for custom status bar integration
        if (lpparam.packageName == "com.android.systemui") {
            try {
                SystemUIHook.inject(lpparam)
                Log.i(TAG, "✅ SystemUI hooked")
            } catch (e: Exception) {
                Log.w(TAG, "SystemUI hook failed (non-critical): ${e.message}")
            }
        }
        
        // Optional: Hook specific apps for spoofing
        if (shouldHookPackage(lpparam.packageName)) {
            PropertySpoofer.hookPackage(lpparam)
        }
    }
    
    private fun shouldHookPackage(packageName: String): Boolean {
        // Hook apps that check device properties
        val targetApps = setOf(
            "com.google.android.gms",
            "com.android.vending",
            "com.google.android.apps.photos",
            "com.netflix.mediaclient",
            "com.tencent.ig"
        )
        return packageName in targetApps
    }
}
