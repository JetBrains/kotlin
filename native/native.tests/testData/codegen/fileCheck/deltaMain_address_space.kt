// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: isAppleTarget=false
// DISABLE_NATIVE: targetArchitecture=X64
// DISABLE_NATIVE: targetArchitecture=X86
// DISABLE_NATIVE: targetArchitecture=ARM32
// DISABLE_NATIVE: targetFamily=WATCHOS
// DISABLE_NATIVE: gcType=NOOP
// DISABLE_NATIVE: gcType=STWMS
// DISABLE_NATIVE: gcType=PMCS
// DISABLE_NATIVE: optimizationMode=DEBUG
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -Xbinary=gcStackMapScheme=delta-main
// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi

// Kotlin objects live in address space 1. The strategy that runs the stack maps is selected per
// function: every function that handles Kotlin objects must name it.

import kotlin.native.NoInline

class A(val x: Int)

// CHECK-LABEL: define ptr addrspace(1) @"kfun:#identity(A){}A"(ptr addrspace(1)
// CHECK-SAME: gc "kotlin-native"
@NoInline
fun identity(a: A): A = a

// CHECK-LABEL: define i32 @"kfun:#field(A){}kotlin.Int"(ptr addrspace(1)
// CHECK-SAME: gc "kotlin-native"
@NoInline
fun field(a: A): Int = a.x

fun box(): String {
    val a = A(7)
    if (field(identity(a)) != 7) return "FAIL"
    return "OK"
}
