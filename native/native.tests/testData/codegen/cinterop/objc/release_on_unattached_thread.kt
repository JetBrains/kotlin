// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

// MODULE: cinterop
// FILE: objclib.def
language = Objective-C
headers = objclib.h

// FILE: objclib.h
#include <objc/NSObject.h>
#include <pthread.h>

static const int OBJECT_COUNT = 10;

static NSObject* globalObjects[OBJECT_COUNT];

void setObjectAt(int index, NSObject* obj) {
    globalObjects[index] = obj;
}

static BOOL ready = NO;

void setReady() {
    __atomic_store_n(&ready, YES, __ATOMIC_RELEASE);
}

static pthread_t threads[OBJECT_COUNT];

void* threadRoutine(void* data) {
    int index = *(int*)data;
    free(data);
    while (__atomic_load_n(&ready, __ATOMIC_ACQUIRE) == NO) {}
    @autoreleasepool {
        globalObjects[index] = nil;
    }
    return NULL;
}

void startThreads() {
    for (int i = 0; i < OBJECT_COUNT; ++i) {
        int* data = malloc(sizeof(int));
        *data = i;
        pthread_create(&threads[i], NULL, threadRoutine, data);
    }
}

void waitThreads() {
    for (int i = 0; i < OBJECT_COUNT; ++i) {
        pthread_join(threads[i], NULL);
    }
}

// MODULE: main(cinterop)
// FILE: main.kt

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.native.runtime.NativeRuntimeApi::class)
import kotlin.native.runtime.GC
import objclib.*

const val REPEAT_COUNT = 5

class C : NSObject()

fun box(): String {
    repeat(REPEAT_COUNT) {
        startThreads()
        repeat(OBJECT_COUNT) {
            setObjectAt(it, C())
        }
        GC.schedule()
        setReady()

        waitThreads()
    }
    return "OK"
}
