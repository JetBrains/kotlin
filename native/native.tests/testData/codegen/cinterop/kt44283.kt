// TARGET_BACKEND: NATIVE
// MODULE: cinterop
// FILE: kt44283.def
---
#include <stdlib.h>
#include <pthread.h>

typedef struct {
    int d;
} TestStruct;

typedef struct {
    void (*f)(TestStruct data);
} ThreadData;

void *dispatch(void *rawArg) {
    ThreadData *arg = rawArg;
    arg->f((TestStruct) {.d = 1});
    return NULL;
}

void invokeFromThread(void (*f)(TestStruct data)) {
    pthread_t thread_id;
    ThreadData *threadData = malloc(sizeof(ThreadData));
    threadData->f = f;
    pthread_create(&thread_id, NULL, dispatch, (void *) threadData);
    pthread_join(thread_id, NULL);
}


// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlin.native.runtime.NativeRuntimeApi::class)

import kotlinx.cinterop.*
import kt44283.*
import kotlin.concurrent.AtomicInt
import kotlin.test.*

val callbackCounter = AtomicInt(0)

@ExperimentalForeignApi
fun box(): String {
    val func = staticCFunction<CValue<TestStruct>, Unit> {
        kotlin.native.runtime.GC.collect() // Helps to ensure that "runtime" is already initialized.

        memScoped {
            println("Hello, Kotlin/Native! ${it.ptr.pointed.d}")
        }
        callbackCounter.incrementAndGet()
    }

    assertEquals(0, callbackCounter.value)
    invokeFromThread(func.reinterpret())
    assertEquals(1, callbackCounter.value)

    return "OK"
}