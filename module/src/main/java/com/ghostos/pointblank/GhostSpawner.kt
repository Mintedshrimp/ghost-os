package com.ghostos.pointblank

import android.util.Log
import java.io.File

/**
 * GhostSpawner - Native fork/execve interface
 * 
 * This class bridges to native code that performs the actual
 * fork() and execve() to spawn the GhostOS launcher with UID 1000.
 */
object GhostSpawner {
    
    private const val TAG = "PointBlank/GhostSpawner"
    private var nativeLoaded = false
    
    init {
        try {
            System.loadLibrary("ghost_injector")
            nativeLoaded = true
            Log.i(TAG, "✅ Native library loaded")
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "❌ Failed to load native library", e)
        }
    }
    
    /**
     * Spawn the GhostOS launcher process.
     * 
     * Native implementation:
     * - fork() creates child process (inherits UID 1000)
     * - Parent returns child PID
     * - Child calls execve() to launch GhostOS Launcher
     * 
     * @return PID of spawned process (>0 in parent, 0 in child, -1 on error)
     */
    @JvmStatic
    external fun spawnNative(): Int
    
    /**
     * Spawn GhostOS with fallback mechanisms
     */
    fun spawn(): Int {
        if (!nativeLoaded) {
            Log.e(TAG, "Native library not loaded, cannot spawn")
            return -1
        }
        
        return try {
            // Ensure launcher APK is installed
            if (!isLauncherInstalled()) {
                Log.w(TAG, "GhostOS Launcher not installed yet")
                // Will retry later via watchdog
                return -2
            }
            
            val pid = spawnNative()
            
            if (pid < 0) {
                Log.e(TAG, "Native spawn failed with code: $pid")
            }
            
            pid
        } catch (e: Exception) {
            Log.e(TAG, "Exception during spawn", e)
            -1
        }
    }
    
    private fun isLauncherInstalled(): Boolean {
        // Check if our package exists
        val packagePath = "/data/app/com.ghostos.launcher-*/base.apk"
        return File("/data/data/com.ghostos.launcher").exists() ||
               File(packagePath).exists()
    }
}
