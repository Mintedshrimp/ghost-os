package com.ghostos.pointblank

import android.util.Log
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * SystemServer Injection Point
 * 
 * This is where we spawn the GhostOS launcher process.
 * SystemServer runs as UID 1000, so any child process inherits UID 1000.
 */
object SystemServerHook {
    
    private const val TAG = "PointBlank/SystemServer"
    private var injected = false
    
    fun inject(lpparam: XC_LoadPackage.LoadPackageParam) {
        if (injected) {
            Log.w(TAG, "Already injected, skipping")
            return
        }
        
        try {
            // Hook SystemServer.startBootstrapServices()
            // This runs early in the boot process, before most services start
            val systemServerClass = XposedHelpers.findClass(
                "com.android.server.SystemServer",
                lpparam.classLoader
            )
            
            XposedHelpers.findAndHookMethod(
                systemServerClass,
                "startBootstrapServices",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        Log.i(TAG, "🚀 SystemServer bootstrap complete - spawning GhostOS...")
                        spawnGhostProcess()
                    }
                }
            )
            
            // Also hook startOtherServices() as backup
            XposedHelpers.findAndHookMethod(
                systemServerClass,
                "startOtherServices",
                object : XC_MethodHook() {
                    override fun afterHookedMethod(param: MethodHookParam) {
                        // Ensure ghost is spawned (idempotent)
                        spawnGhostProcess()
                    }
                }
            )
            
            // Hook ActivityManagerService to protect our process
            hookActivityManager(lpparam)
            
            injected = true
            Log.i(TAG, "✅ SystemServer hooks installed")
            XposedBridge.log("Point Blank: SystemServer hooks active")
            
        } catch (e: Exception) {
            Log.e(TAG, "SystemServer injection failed", e)
            XposedBridge.log("Point Blank: SystemServer hook failed - ${e.message}")
        }
    }
    
    private fun spawnGhostProcess() {
        try {
            val pid = GhostSpawner.spawn()
            
            if (pid > 0) {
                Log.i(TAG, "✅ GhostOS process spawned with PID: $pid (UID: 1000)")
                XposedBridge.log("Point Blank: GhostOS spawned (PID: $pid)")
                
                // Store PID for watchdog
                GhostWatchdog.registerGhostPid(pid)
            } else if (pid == 0) {
                // This code runs in the child process
                // We should never reach here in Kotlin - native code handles execve()
                Log.i(TAG, "👻 Inside ghost process!")
            } else {
                Log.e(TAG, "❌ Failed to spawn ghost process")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception spawning ghost process", e)
        }
    }
    
    /**
     * Hook ActivityManagerService to protect GhostOS from being killed
     */
    private fun hookActivityManager(lpparam: XC_LoadPackage.LoadPackageParam) {
        try {
            val amsClass = XposedHelpers.findClass(
                "com.android.server.am.ActivityManagerService",
                lpparam.classLoader
            )
            
            // Hook process kill methods to protect GhostOS
            XposedHelpers.findAndHookMethod(
                amsClass,
                "killPackageProcesses",
                String::class.java,
                Int::class.java,
                object : XC_MethodHook() {
                    override fun beforeHookedMethod(param: MethodHookParam) {
                        val packageName = param.args[0] as String
                        if (packageName == "com.ghostos.launcher") {
                            param.result = null
                            Log.w(TAG, "Blocked attempt to kill GhostOS!")
                        }
                    }
                }
            )
            
            Log.i(TAG, "✅ AMS protection hooks installed")
        } catch (e: Exception) {
            Log.w(TAG, "AMS protection hook failed: ${e.message}")
        }
    }
}
