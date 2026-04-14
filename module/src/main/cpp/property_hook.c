/**
 * property_hook.c - Native Property Interception
 * 
 * Hooks the native property_get function to catch cached property reads
 * that bypass the Java layer hooks.
 */

#include <jni.h>
#include <string.h>
#include <android/log.h>
#include <dlfcn.h>
#include <sys/system_properties.h>

#define LOG_TAG "PropertyHook"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

// Original property_get function pointer
static int (*original_property_get)(const char *, char *, const char *) = NULL;

// Spoofed property mappings (simple static map for demo)
static const struct {
    const char *key;
    const char *value;
} spoofed_props[] = {
    {"ro.product.model", "Pixel 9 Pro"},
    {"ro.product.brand", "google"},
    {"ro.product.manufacturer", "Google"},
    {"ro.build.fingerprint", "google/komodo/komodo:15/AP31.240617.009/12345678:user/release-keys"},
    {NULL, NULL}
};

/**
 * Our hooked version of property_get
 */
static int hooked_property_get(const char *key, char *value, const char *default_value) {
    
    // Check if we should spoof this property
    for (int i = 0; spoofed_props[i].key != NULL; i++) {
        if (strcmp(key, spoofed_props[i].key) == 0) {
            if (value) {
                strcpy(value, spoofed_props[i].value);
            }
            return strlen(spoofed_props[i].value);
        }
    }
    
    // Not spoofed, call original
    if (original_property_get) {
        return original_property_get(key, value, default_value);
    }
    
    // Fallback to system call
    return __system_property_get(key, value);
}

/**
 * Initialize native property hook
 * 
 * Note: This is a simplified version. Real implementation would use
 * inline hooking (PLT/GOT hooking) to intercept property_get calls.
 */
__attribute__((constructor))
static void init_property_hook(void) {
    LOGI("🔧 Initializing native property hook...");
    
    // Get original function
    original_property_get = (int (*)(const char *, char *, const char *))
        dlsym(RTLD_NEXT, "property_get");
    
    if (!original_property_get) {
        original_property_get = __system_property_get;
    }
    
    LOGI("✅ Native property hook ready");
    
    // Note: Actual hook installation would require more complex inline hooking
    // This is a placeholder showing the concept
}
