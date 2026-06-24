/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*

const val N = 16

// The default `clear = true` must zero out the freshly allocated memory.
fun defaultIsCleared(): Boolean {
    @StackAlloc(N) lateinit var arr: CPointer<IntVar>
    for (i in 0..<N)
        if (arr[i] != 0) return false
    return true
}

// An explicit `clear = true` must zero out the memory.
fun explicitClear(): Boolean {
    @StackAlloc(N, clear = true) lateinit var arr: CPointer<LongVar>
    for (i in 0..<N)
        if (arr[i] != 0L) return false
    return true
}

// Writes into the stack buffer must round-trip.
fun writeRead(): Int {
    @StackAlloc(8) lateinit var arr: CPointer<IntVar>
    for (i in 0..<8)
        arr[i] = i * i
    var sum = 0
    for (i in 0 until 8)
        sum += arr[i]
    return sum // 0 + 1 + 4 + 9 + 16 + 25 + 36 + 49 = 140
}

// `clear = false` skips zeroing, but the buffer must still be usable.
fun noClearUsable(): Float {
    @StackAlloc(4, clear = false) lateinit var arr: CPointer<FloatVar>
    for (i in 0..<4)
        arr[i] = (i + 1).toFloat()
    var sum = 0.0f
    for (i in 0..<4)
        sum += arr[i]
    return sum // 1 + 2 + 3 + 4 = 10
}

// Two stack allocations within the same function must not alias.
fun independentBuffers(): Boolean {
    @StackAlloc(4) lateinit var a: CPointer<IntVar>
    @StackAlloc(4) lateinit var b: CPointer<IntVar>
    for (i in 0..<4) a[i] = 1
    for (i in 0..<4) b[i] = 2
    for (i in 0..<4)
        if (a[i] != 1 || b[i] != 2) return false
    return true
}

fun box(): String {
    if (!defaultIsCleared()) return "FAIL: default clear did not zero memory"
    if (!explicitClear()) return "FAIL: explicit clear did not zero memory"

    val sum = writeRead()
    if (sum != 140) return "FAIL: writeRead sum=$sum"

    val fsum = noClearUsable()
    if (fsum != 10.0f) return "FAIL: noClear sum=$fsum"

    if (!independentBuffers()) return "FAIL: stack buffers alias"

    return "OK"
}
