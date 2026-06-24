/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

import kotlinx.cinterop.*

// @StackAlloc must lower to a raw LLVM `alloca` of the array type.
// `[17 x i32]` spans 68 bytes; 68 is not a multiple of the pointer size, so the zeroing
// memset emitted for `clear = true` cannot be confused with the GC frame-slot memset
// (which always zeroes a `ptr`-array, i.e. a multiple of 8 bytes).
// CHECK-LABEL: define {{.*}}@"kfun:#cleared(){}kotlin.Int"
// CHECK: alloca [17 x i32]
// CHECK: call void @llvm.memset.p0.i32(ptr {{.*}}, i8 0, i32 68,
fun cleared(): Int {
    @StackAlloc(17) lateinit var arr: CPointer<IntVar>
    arr[0] = 42
    return arr[0]
}

// With `clear = false` the `alloca` is still emitted, but the 68-byte zeroing memset must be absent.
// CHECK-LABEL: define {{.*}}@"kfun:#notCleared(){}kotlin.Int"
// CHECK: alloca [17 x i32]
// CHECK-NOT: call void @llvm.memset.p0.i32(ptr {{.*}}, i8 0, i32 68,
// CHECK-LABEL: define {{.*}}@"kfun:#box(){}kotlin.String"
fun notCleared(): Int {
    @StackAlloc(17, clear = false) lateinit var arr: CPointer<IntVar>
    arr[0] = 7
    return arr[0]
}

fun box(): String {
    if (cleared() != 42) return "FAIL"
    if (notCleared() != 7) return "FAIL"
    return "OK"
}
