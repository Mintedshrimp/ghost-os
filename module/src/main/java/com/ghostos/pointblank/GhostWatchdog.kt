package com.ghostos.pointblank

import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import java.io.File

/**
 * GhostWatchdog - Process Persistence
 * 
 * Monitors the GhostOS launcher process and respawns it if killed.
 * Runs in SystemServer context so it cannot be killed by normal apps.
 */
object GhostWatchdog {
    
    private const val TAG = "PointBlank/Watchdog"
    private const val CHECK_INTERVAL_MS = 5000L  // Check every 5 seconds
    private const val MAX_RESPAWN_ATTEMPTS = 5
    
    private var watchThread: HandlerThread? = null
    private var handler: Handler? = null
    private var ghostPid: Int = -1
    private var respawnAttempts = 0
    private var running = false
    
    fun registerGhostPid(pid: Int) {
        ghostPid = pid
        respawnAttempts = 0
        Log.i(TAG, "Registered GhostOS PID: $pid")
        startWatching()
    }
    
    private fun startWatching() {
        if (running) return
        
        watchThread = HandlerThread("GhostWatchdog").apply { start() }
        handler = Handler(watchThread!!.looper)
        running = true
        
        handler?.post(object : Runnable {
            override fun run() {
                checkGhostProcess()
                if (running) {
                    handler?.postDelayed(this, CHECK_INTERVAL_MS)
                }
            }
        })
        
        Log.i(TAG, "👁️ Watchdog started")
    }
    
    private fun checkGhostProcess() {
        if (ghostPid <= 0) {
            // No PID registered yet, try to find it
            findGhostProcess()
            return
        }
        
        // Check if process still exists
        val procDir = File("/proc/$ghostPid")
        if (!procDir.exists()) {
            Log.w(TAG, "⚠️ GhostOS process (PID: $ghostPid) died!")
            handleProcessDeath()
        } else {
            // Process alive - check if it's still our launcher
            try {
                val cmdline = File("/proc/$ghostPid/cmdline").readText()
                if (!cmdline.contains("com.ghostos.launcher")) {
                    Log.w(TAG, "PID $ghostPid is not GhostOS anymore")
                    handleProcessDeath()
                }
            } catch (e: Exception) {
                // Process probably died between check and read
                handleProcessDeath()
            }
        }
    }
    
    private fun findGhostProcess() {
        try {
            val procDir = File("/proc")
            procDir.listFiles()?.forEach { dir ->
                if (dir.name.matches(Regex("\\d+"))) {
                    try {
                        val cmdline = File(dir, "cmdline").readText()
                        if (cmdline.contains("com.ghostos.launcher")) {
                            val pid = dir.name.toInt()
                            ghostPid = pid
                            Log.i(TAG, "Found GhostOS process: PID $pid")
                            return
                        }
                    } catch (e: Exception) {
                        // Skip
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to scan for GhostOS: ${e.message}")
        }
    }
    
    private fun handleProcessDeath() {
        ghostPid = -1
        
        if (respawnAttempts >= MAX_RESPAWN_ATTEMPTS) {
            Log.e(TAG, "❌ Max respawn attempts reached. Giving up.")
            // Will retry after a longer delay
            respawnAttempts = 0
            return
        }
        
        respawnAttempts++
        Log.i(TAG, "🔄 Respawning GhostOS (attempt $respawnAttempts/$MAX_RESPAWN_ATTEMPTS)")
        
        // Try to respawn
        val newPid = GhostSpawner.spawn()
        
        if (newPid > 0) {
            ghostPid = newPid
            respawnAttempts = 0
            Log.i(TAG, "✅ GhostOS respawned with PID: $newPid")
        } else {
            Log.e(TAG, "❌ Respawn failed")
        }
    }
    
    fun stop() {
        running = false
        handler?.removeCallbacksAndMessages(null)
        watchThread?.quitSafely()
        Log.i(TAG, "Watchdog stopped")
    }
}
