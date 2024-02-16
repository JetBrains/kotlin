// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

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
    // Experimental MM supports arbitrary object sharing.
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
