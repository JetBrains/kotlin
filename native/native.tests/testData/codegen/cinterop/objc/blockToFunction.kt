// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false

// MODULE: cinterop
// FILE: lib.def
language = Objective-C
headers = lib.h
headerFilter = lib.h

// FILE: lib.h
#include <objc/objc.h>
#include <Foundation/NSArray.h>

static void* getCapturedPtrBlock(void* ptr) {
    return (__bridge_retained void*)^void* (void) {
        return ptr;
    };
}

static void* getIntToBoolBlock() {
    return (__bridge_retained void*)^BOOL (int p) {
        return p != 0;
    };
}

static void* getVoidBlock(int* callsCount) {
    return (__bridge_retained void*)^void (void) {
        ++*callsCount;
    };
}

static void* getBlockCompositorBlock() {
    return (__bridge_retained void*)^ (int (^f)(int), int (^g)(int)) {
        return ^int (int p) {
            return f(g(p));
        };
    };
}

static void* getNSArrayOfStringsBlock() {
    return (__bridge_retained void*)^NSArray* (NSString* s1, NSString* s2, NSString* s3) {
        return @[s1, s2, s3];
    };
}

static void callKotlinWithStackAllocatedBlock(
    int blockResult,
    void (^kotlinBlock)(void*)
) {
    kotlinBlock((__bridge void*)^{ return blockResult; });
}

// MODULE: main(cinterop)
// FILE: main.kt
@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
import lib.*
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

// This function runs [block] and checks that it didn't use an autoreleasepool.
inline fun <R> noAutorelease(block: () -> R): R {
    // Just in case our magic doesn't work, let's at least flush autoreleasepool after the end,
    // to detect over-release via autorelease.
    autoreleasepool {
        val poolBefore = objc_autoreleasePoolPush()
        objc_autoreleasePoolPop(poolBefore)

        val result = block()

        val poolAfter = objc_autoreleasePoolPush()
        objc_autoreleasePoolPop(poolAfter)

        // autoreleasepool machinery is implemented as a stack.
        // So here we check that stack top after `block()` is the same as before.
        // Which means that no objects were added to this stack inbetween.
        check(poolBefore == poolAfter) { "The code used an autoreleasepool; $poolBefore -> $poolAfter" }

        return result
    }
}

fun <R> convert(getBlockPtr: () -> COpaquePointer?, convert: (NativePtr)->R): R = noAutorelease {
    val blockPtr = getBlockPtr()!!.rawValue
    val result = convert(blockPtr)
    objc_release(blockPtr) // Balance __bridge_retained.
    return result
}

fun captureAndReturnPtr(): String {
    val ptr = (null as CPointer<IntVar>?) + 1
    val kotlinFun = convert({ getCapturedPtrBlock(ptr) }) {
        convertBlockPtrToKotlinFunction<()->COpaquePointer?>(it)
    }

    val result = kotlinFun()
    if (result != ptr) return "FAIL: $result"

    return "OK"
}

fun intToBool(): String {
    val kotlinFun = convert(::getIntToBoolBlock) {
        convertBlockPtrToKotlinFunction<(Int)->Boolean>(it)
    }

    if (!kotlinFun(5)) return "FAIL 1"
    if (kotlinFun(0)) return "FAIL 2"

    return "OK"
}

fun void(): String = memScoped {
    val callsCount = alloc<IntVar>()
    val kotlinFun: () -> Any? = convert({ getVoidBlock(callsCount.ptr) }) {
        convertBlockPtrToKotlinFunction<()->Unit>(it)
    }

    if (callsCount.value != 0) return "FAIL 1: ${callsCount.value}"
    val result = kotlinFun()
    // Check that the function is actually called:
    if (callsCount.value != 1) return "FAIL 2: ${callsCount.value}"
    if (result !== Unit) return "FAIL 3: $result"

    return "OK"
}

fun blockParametersAndReturnValues(): String {
    val kotlinFun = convert(::getBlockCompositorBlock) {
        convertBlockPtrToKotlinFunction<((Int)->Int, (Int)->Int)->(Int)->Int>(it)
    }

    val composed = kotlinFun({ it + 1 }, { it * it })
    val result = composed(3)

    return if (result != 10) "FAIL: $result" else "OK"
}

fun NSArrayAndNSString(): String {
    val kotlinFun = convert(::getNSArrayOfStringsBlock) {
        convertBlockPtrToKotlinFunction<(String, String, String) -> List<String>>(it)
    }

    val result = kotlinFun("a", "b", "c")

    return if (result != listOf("a", "b", "c")) "FAIL: $result" else "OK"
}

fun stackBlock(): String {
    val result = StringBuilder()

    var firstBlockPtr: NativePtr = NativePtr.NULL
    lateinit var firstBlockConverted: () -> Int

    callKotlinWithStackAllocatedBlock(42) {
        firstBlockPtr = it!!.rawValue
        firstBlockConverted = noAutorelease {
            convertBlockPtrToKotlinFunction<() -> Int>(firstBlockPtr)
        }
    }

    callKotlinWithStackAllocatedBlock(11) {
        val secondBlockPtr = it!!.rawValue
        val secondBlockConverted = noAutorelease {
            convertBlockPtrToKotlinFunction<()->Int>(secondBlockPtr)
        }

        // Blocks are allocated at the same place on the stack:
        if (firstBlockPtr != secondBlockPtr)
            result.appendLine("FAIL 1: $firstBlockPtr != $secondBlockPtr")

        // But Kotlin functions are two different objects:
        val fortyTwo = firstBlockConverted()
        if (fortyTwo != 42)
            result.appendLine("FAIL 2: $fortyTwo")

        val eleven = secondBlockConverted()
        if (eleven != 11)
            result.appendLine("FAIL 3: $eleven")

        // Which should mean that the conversions properly copies the block.
    }

    result.append("OK")
    return result.toString()
}

fun box(): String {
    val results = buildList {
        add(captureAndReturnPtr())
        add(intToBool())
        add(void())
        add(blockParametersAndReturnValues())
        add(NSArrayAndNSString())
        add(stackBlock())
    }

    return if (results.any { it != "OK" }) results.joinToString() else "OK"
}