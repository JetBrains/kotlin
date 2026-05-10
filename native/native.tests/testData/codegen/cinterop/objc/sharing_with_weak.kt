// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.3
// ^^^ KT-86026 TODO: Rework testdata to move functions/globals definitions from .def/.h into separate source files

// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// WITH_PLATFORM_LIBS

// MODULE: cinterop
// FILE: objclib.def
language = Objective-C
headers = objclib.h
headerFilter = objclib.h

// FILE: objclib.h
#import <objc/NSObject.h>

static NSObject* __weak globalObject = nil;

void setObject(NSObject* obj) {
    globalObject = obj;
}

bool isObjectAlive() {
    return globalObject != nil;
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(ObsoleteWorkersApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)
import kotlin.native.concurrent.*
import kotlin.test.*
import kotlinx.cinterop.autoreleasepool
import objclib.*

fun box(): String {
    val success = autoreleasepool {
        run()
    }
    return if (success) "OK" else "FAIL"
}

private class NSObjectImpl : NSObject() {
    var x = 111
}

fun run() = withWorker {
    val obj = NSObjectImpl()
    setObject(obj)

    try {
        execute(TransferMode.SAFE, {}) {
            isObjectAlive()
        }.result
    } catch (e: Throwable) {
        false
    }
}
