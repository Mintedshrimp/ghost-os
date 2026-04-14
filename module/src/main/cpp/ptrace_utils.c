/**
 * ptrace_utils.c - Process Injection Utilities
 * 
 * Provides functions for attaching to and manipulating other processes.
 * Used by GhostLab for runtime code injection.
 */

#include <jni.h>
#include <unistd.h>
#include <sys/ptrace.h>
#include <sys/wait.h>
#include <sys/mman.h>
#include <android/log.h>
#include <string.h>
#include <errno.h>

#define LOG_TAG "PtraceUtils"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

/**
 * Attach to a process via ptrace
 */
JNIEXPORT jint JNICALL
Java_com_ghostos_pointblank_ProcessInjector_ptraceAttach(JNIEnv *env, jobject thiz, jint pid) {
    
    if (ptrace(PTRACE_ATTACH, pid, NULL, NULL) == -1) {
        LOGE("ptrace attach failed: %s", strerror(errno));
        return -1;
    }
    
    int status;
    waitpid(pid, &status, 0);
    
    if (WIFSTOPPED(status)) {
        LOGI("✅ Attached to PID %d", pid);
        return 0;
    }
    
    return -1;
}

/**
 * Detach from a process
 */
JNIEXPORT jint JNICALL
Java_com_ghostos_pointblank_ProcessInjector_ptraceDetach(JNIEnv *env, jobject thiz, jint pid) {
    
    if (ptrace(PTRACE_DETACH, pid, NULL, NULL) == -1) {
        LOGE("ptrace detach failed: %s", strerror(errno));
        return -1;
    }
    
    LOGI("✅ Detached from PID %d", pid);
    return 0;
}

/**
 * Read memory from a process
 */
JNIEXPORT jbyteArray JNICALL
Java_com_ghostos_pointblank_ProcessInjector_readMemory(
    JNIEnv *env, jobject thiz, jint pid, jlong address, jint size) {
    
    // Allocate buffer
    unsigned char *buffer = (unsigned char *)malloc(size);
    if (!buffer) {
        return NULL;
    }
    
    // Read memory word by word
    for (int i = 0; i < size; i += sizeof(long)) {
        long data = ptrace(PTRACE_PEEKDATA, pid, (void *)(address + i), NULL);
        if (data == -1 && errno != 0) {
            LOGE("ptrace peek failed at 0x%lx: %s", (long)(address + i), strerror(errno));
            free(buffer);
            return NULL;
        }
        
        size_t copy_size = (size - i) < sizeof(long) ? (size - i) : sizeof(long);
        memcpy(buffer + i, &data, copy_size);
    }
    
    // Convert to Java byte array
    jbyteArray result = (*env)->NewByteArray(env, size);
    (*env)->SetByteArrayRegion(env, result, 0, size, (jbyte *)buffer);
    
    free(buffer);
    return result;
}

/**
 * Write memory to a process
 */
JNIEXPORT jint JNICALL
Java_com_ghostos_pointblank_ProcessInjector_writeMemory(
    JNIEnv *env, jobject thiz, jint pid, jlong address, jbyteArray data) {
    
    jsize size = (*env)->GetArrayLength(env, data);
    jbyte *bytes = (*env)->GetByteArrayElements(env, data, NULL);
    
    // Write memory word by word
    for (int i = 0; i < size; i += sizeof(long)) {
        long word = 0;
        size_t copy_size = (size - i) < sizeof(long) ? (size - i) : sizeof(long);
        memcpy(&word, bytes + i, copy_size);
        
        if (ptrace(PTRACE_POKEDATA, pid, (void *)(address + i), (void *)word) == -1) {
            LOGE("ptrace poke failed at 0x%lx: %s", (long)(address + i), strerror(errno));
            (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
            return -1;
        }
    }
    
    (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);
    LOGI("✅ Wrote %d bytes to PID %d at 0x%lx", size, pid, (long)address);
    return 0;
}

/**
 * Find a pattern in process memory
 */
JNIEXPORT jlong JNICALL
Java_com_ghostos_pointblank_ProcessInjector_findPattern(
    JNIEnv *env, jobject thiz, jint pid, jlong start, jlong end, jbyteArray pattern) {
    
    // Simplified implementation - real version would be more efficient
    jsize pattern_size = (*env)->GetArrayLength(env, pattern);
    jbyte *pattern_bytes = (*env)->GetByteArrayElements(env, pattern, NULL);
    
    unsigned char *buffer = (unsigned char *)malloc(4096);
    jlong found = -1;
    
    for (jlong addr = start; addr < end; addr += 4096) {
        jbyteArray chunk = Java_com_ghostos_pointblank_ProcessInjector_readMemory(
            env, thiz, pid, addr, 4096);
        
        if (chunk) {
            jbyte *chunk_bytes = (*env)->GetByteArrayElements(env, chunk, NULL);
            
            // Simple pattern search
            for (int i = 0; i <= 4096 - pattern_size; i++) {
                if (memcmp(chunk_bytes + i, pattern_bytes, pattern_size) == 0) {
                    found = addr + i;
                    break;
                }
            }
            
            (*env)->ReleaseByteArrayElements(env, chunk, chunk_bytes, JNI_ABORT);
        }
        
        if (found != -1) break;
    }
    
    (*env)->ReleaseByteArrayElements(env, pattern, pattern_bytes, JNI_ABORT);
    free(buffer);
    
    return found;
}
