/**
 * ghost_injector.c - The Core Fork/Execve Implementation
 * 
 * This is the heart of Point Blank. It forks from SystemServer and
 * launches the GhostOS Launcher with inherited UID 1000 privileges.
 */

#include <jni.h>
#include <unistd.h>
#include <stdlib.h>
#include <string.h>
#include <sys/prctl.h>
#include <sys/wait.h>
#include <android/log.h>
#include <dlfcn.h>
#include <fcntl.h>
#include <errno.h>

#define LOG_TAG "GhostInjector"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

// SELinux context to switch to (optional)
#define TARGET_SELINUX_CONTEXT "u:r:platform_app:s0"

// Forward declarations
static int set_selinux_context(const char *context);
static char* find_launcher_apk(void);
static int prepare_environment(void);

/**
 * Native implementation of GhostSpawner.spawnNative()
 * 
 * @return PID of child (>0) in parent, 0 in child, -1 on error
 */
JNIEXPORT jint JNICALL
Java_com_ghostos_pointblank_GhostSpawner_spawnNative(JNIEnv *env, jobject thiz) {
    
    LOGI("👻 GhostSpawner: Preparing to fork...");
    
    // Prepare environment before fork
    if (prepare_environment() != 0) {
        LOGW("Environment preparation had issues, continuing anyway");
    }
    
    // THE FORK - This is where the magic happens
    pid_t pid = fork();
    
    if (pid < 0) {
        LOGE("❌ Fork failed: %s", strerror(errno));
        return -1;
    }
    
    if (pid == 0) {
        // ============================================================
        // CHILD PROCESS
        // ============================================================
        // We inherit UID 1000, GID 1000, and SystemServer's capabilities
        // This is the ghost process that will become the launcher
        
        LOGI("👻 Child process born! PID: %d, UID: %d", getpid(), getuid());
        
        // Detach from parent's process group
        setsid();
        
        // Change process name to hide from casual inspection
        prctl(PR_SET_NAME, "com.ghostos.launcher", 0, 0, 0);
        
        // Optional: Change SELinux context to blend in with normal apps
        if (set_selinux_context(TARGET_SELINUX_CONTEXT) != 0) {
            LOGW("Failed to change SELinux context, continuing with inherited context");
        }
        
        // Find the launcher APK path
        char *apk_path = find_launcher_apk();
        if (!apk_path) {
            LOGE("❌ Could not find launcher APK!");
            exit(1);
        }
        
        LOGI("📦 Launcher APK: %s", apk_path);
        
        // Build CLASSPATH environment variable
        char classpath[1024];
        snprintf(classpath, sizeof(classpath), "CLASSPATH=%s", apk_path);
        free(apk_path);
        
        // Build arguments for app_process
        char *argv[] = {
            "app_process",
            "/system/bin",
            "--nice-name=com.ghostos.launcher",
            "com.ghostos.launcher.GhostLauncherActivity",
            NULL
        };
        
        // Build environment
        char *envp[] = {
            classpath,
            "LD_LIBRARY_PATH=/system/lib64:/system/lib",
            "ANDROID_DATA=/data",
            "ANDROID_ROOT=/system",
            NULL
        };
        
        LOGI("🚀 Executing app_process...");
        
        // Execute the Android runtime with our launcher
        execve("/system/bin/app_process", argv, envp);
        
        // If we reach here, execve failed
        LOGE("❌ execve failed: %s", strerror(errno));
        exit(1);
        
    } else {
        // ============================================================
        // PARENT PROCESS (SystemServer)
        // ============================================================
        
        LOGI("✅ Ghost process spawned! PID: %d", pid);
        
        // Optional: wait a moment to see if child crashes immediately
        int status;
        pid_t result = waitpid(pid, &status, WNOHANG);
        
        if (result == pid) {
            // Child already exited
            if (WIFEXITED(status)) {
                LOGE("❌ Child exited immediately with code: %d", WEXITSTATUS(status));
            } else if (WIFSIGNALED(status)) {
                LOGE("❌ Child killed by signal: %d", WTERMSIG(status));
            }
            return -1;
        }
        
        return pid;
    }
}

/**
 * Change SELinux context of current process
 */
static int set_selinux_context(const char *context) {
    int (*setcon_func)(const char *) = NULL;
    void *handle = dlopen("libselinux.so", RTLD_NOW);
    
    if (!handle) {
        LOGW("Could not load libselinux.so");
        return -1;
    }
    
    setcon_func = (int (*)(const char *))dlsym(handle, "setcon");
    
    if (!setcon_func) {
        LOGW("setcon symbol not found");
        dlclose(handle);
        return -1;
    }
    
    int result = setcon_func(context);
    dlclose(handle);
    
    if (result != 0) {
        LOGW("setcon failed: %s", strerror(errno));
        return -1;
    }
    
    LOGI("✅ SELinux context changed to: %s", context);
    return 0;
}

/**
 * Find the installed launcher APK path
 */
static char* find_launcher_apk(void) {
    const char *search_paths[] = {
        "/data/app/com.ghostos.launcher-*/base.apk",
        "/data/app/~~*/com.ghostos.launcher-*/base.apk",
        "/data/app/com.ghostos.launcher/base.apk",
        NULL
    };
    
    // Use shell to expand wildcards
    for (int i = 0; search_paths[i] != NULL; i++) {
        char cmd[512];
        char result[512];
        FILE *fp;
        
        snprintf(cmd, sizeof(cmd), "ls %s 2>/dev/null | head -1", search_paths[i]);
        fp = popen(cmd, "r");
        
        if (fp && fgets(result, sizeof(result), fp) != NULL) {
            pclose(fp);
            
            // Remove trailing newline
            size_t len = strlen(result);
            if (len > 0 && result[len-1] == '\n') {
                result[len-1] = '\0';
            }
            
            if (strlen(result) > 0) {
                return strdup(result);
            }
        }
        
        if (fp) pclose(fp);
    }
    
    return NULL;
}

/**
 * Prepare environment for the ghost process
 */
static int prepare_environment(void) {
    // Ensure /data/local/tmp exists for our files
    mkdir("/data/local/tmp/ghost", 0755);
    
    // Create a marker file to indicate GhostOS is active
    int fd = open("/data/local/tmp/ghost/active", O_CREAT | O_WRONLY | O_TRUNC, 0644);
    if (fd >= 0) {
        char buf[32];
        snprintf(buf, sizeof(buf), "%ld", (long)time(NULL));
        write(fd, buf, strlen(buf));
        close(fd);
    }
    
    return 0;
}

// JNI_OnLoad - Called when library is loaded
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void *reserved) {
    LOGI("🔫 Point Blank native library loaded");
    return JNI_VERSION_1_6;
}
