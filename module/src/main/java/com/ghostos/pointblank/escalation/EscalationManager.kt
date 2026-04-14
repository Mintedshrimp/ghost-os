package com.ghostos.pointblank.escalation

import android.content.Context
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Manages all UID 1000 escalation methods with automatic fallback.
 * 
 * Priority order:
 * 1. PointBlank (Primary, 95%)
 * 2. SamsungTTS (Samsung only, 95%)
 * 3. ZygoteInjection (Pre-June 2024 patch, 40%)
 * 4. TestKeyEscalation (Test-key ROMs, 30%)
 * 5. BinderHijack (Legacy, 5%)
 * 6. ShizukuFallback (UID 2000, 80%)
 */
object EscalationManager {
    
    private const val TAG = "GhostOS/EscalationManager"
    
    private val isEscalating = AtomicBoolean(false)
    private var currentMethod: EscalationMethod? = null
    private var currentUid: Int = -1
    
    private lateinit var methods: List<EscalationMethod>
    
    /**
     * Initialize the escalation manager with all available methods.
     */
    fun initialize(context: Context) {
        methods = listOf(
            PointBlankEscalation(),
            SamsungTTSEscalation(),
            ZygoteInjectionEscalation(),
            TestKeyEscalation(),
            BinderHijackEscalation(),
            ShizukuFallback()
        ).sortedBy { it.priority.ordinal }
        
        Log.i(TAG, "Initialized with ${methods.size} escalation methods")
        methods.forEach { method ->
            Log.i(TAG, "  - ${method.name} (Priority: ${method.priority}, Success: ${method.estimatedSuccessRate}%)")
        }
    }
    
    /**
     * Attempt to achieve UID 1000 using the best available method.
     * Returns true if any method succeeded (UID 1000 or UID 2000).
     */
    fun escalate(context: Context): Boolean {
        if (isEscalating.getAndSet(true)) {
            Log.w(TAG, "Escalation already in progress")
            return false
        }
        
        try {
            // Check if we already have UID 1000
            val currentUid = getCurrentUid()
            if (currentUid == 1000) {
                Log.i(TAG, "Already running as UID 1000")
                return true
            }
            
            Log.i(TAG, "Starting escalation process (Current UID: $currentUid)")
            
            for (method in methods) {
                Log.i(TAG, "Trying method: ${method.name}")
                
                // Check if method is available for this device
                if (!method.isAvailable(context)) {
                    Log.i(TAG, "  └─ Not available: ${method.getFailureReason()}")
                    continue
                }
                
                // Attempt execution
                val result = method.execute(context)
                
                when (result) {
                    EscalationMethod.Result.SUCCESS_UID1000 -> {
                        currentMethod = method
                        this.currentUid = 1000
                        Log.i(TAG, "✅ SUCCESS! UID 1000 achieved via ${method.name}")
                        return true
                    }
                    EscalationMethod.Result.SUCCESS_UID2000 -> {
                        currentMethod = method
                        this.currentUid = 2000
                        Log.i(TAG, "⚠ Partial success: UID 2000 achieved via ${method.name}")
                        return true
                    }
                    else -> {
                        Log.w(TAG, "  └─ Failed: ${method.getFailureReason()}")
                    }
                }
            }
            
            Log.e(TAG, "❌ All escalation methods failed")
            return false
            
        } finally {
            isEscalating.set(false)
        }
    }
    
    /**
     * Get the current UID of the process.
     */
    fun getCurrentUid(): Int {
        if (currentUid > 0) return currentUid
        currentUid = android.os.Process.myUid()
        return currentUid
    }
    
    /**
     * Get the name of the currently active escalation method.
     */
    fun getActiveMethodName(): String {
        return currentMethod?.name ?: "None"
    }
    
    /**
     * Get detailed status for UI display.
     */
    fun getStatus(): EscalationStatus {
        return EscalationStatus(
            uid = getCurrentUid(),
            activeMethod = currentMethod?.name,
            isFullPrivilege = getCurrentUid() == 1000,
            availableMethods = methods.filter { it.isAvailable(getContext()) }.map { it.name },
            failedMethods = methods.filter { !it.isAvailable(getContext()) }.map { it.name }
        )
    }
    
    private lateinit var appContext: Context
    private fun getContext(): Context = appContext
    
    data class EscalationStatus(
        val uid: Int,
        val activeMethod: String?,
        val isFullPrivilege: Boolean,
        val availableMethods: List<String>,
        val failedMethods: List<String>
    )
}
