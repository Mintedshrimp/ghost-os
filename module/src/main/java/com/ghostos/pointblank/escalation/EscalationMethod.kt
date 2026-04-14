package com.ghostos.pointblank.escalation

import android.content.Context
import android.util.Log

/**
 * Abstract base class for all UID 1000 escalation methods.
 * Each method implements its own availability check and execution logic.
 */
abstract class EscalationMethod {
    
    companion object {
        const val TAG = "GhostOS/Escalation"
    }
    
    enum class Priority {
        PRIMARY,      // Point Blank - first attempt
        SECONDARY,    // Samsung TTS, Zygote Injection
        FALLBACK,     // Shizuku (UID 2000)
        LEGACY        // Old methods, low success
    }
    
    enum class Result {
        SUCCESS_UID1000,      // Full system privileges
        SUCCESS_UID2000,      // ADB shell privileges (Shizuku)
        FAILED_NOT_AVAILABLE, // Method not applicable to this device
        FAILED_BLOCKED,       // Method available but blocked (SELinux, etc)
        FAILED_UNKNOWN        // Execution failed for unknown reason
    }
    
    abstract val name: String
    abstract val priority: Priority
    abstract val estimatedSuccessRate: Int  // Percentage
    
    /**
     * Check if this method is applicable to the current device.
     * For example, Samsung TTS only returns true on Samsung devices.
     */
    abstract fun isAvailable(context: Context): Boolean
    
    /**
     * Attempt to execute this escalation method.
     * Returns the result of the attempt.
     */
    abstract fun execute(context: Context): Result
    
    /**
     * Get detailed failure reason for logging/debugging.
     */
    abstract fun getFailureReason(): String
    
    /**
     * Clean up any resources if escalation fails or is no longer needed.
     */
    open fun cleanup() {}
    
    protected fun log(message: String) {
        Log.i(TAG, "[$name] $message")
    }
    
    protected fun logError(message: String, e: Exception? = null) {
        if (e != null) {
            Log.e(TAG, "[$name] $message", e)
        } else {
            Log.e(TAG, "[$name] $message")
        }
    }
}
