// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

// MODULE: cinterop
// FILE: lib.def
language = Objective-C
headers = lib.h
headerFilter = lib.h

// FILE: lib.h

void* getBlockPtr(int x) {
    return (__bridge_retained void*)^int*(int* p) {
        return p + x;
    };
}


// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import lib.*
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction
import kotlin.test.*

fun box(): String {
    val blockPtr = getBlockPtr(1)!!
    val kotlinFun = convertBlockPtrToKotlinFunction<(COpaquePointer?)->COpaquePointer?>(blockPtr.rawValue)
    val result = kotlinFun((NativePtr.NULL + 4).toLong().toCPointer())
    if (result?.getRawValue() != NativePtr.NULL + 8) return "FAIL: $result"

    return "OK"
}
