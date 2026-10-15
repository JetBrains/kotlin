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
// FILECHECK_STAGE: LTOBitcodeOptimization
// FREE_COMPILER_ARGS: -Xbinary=gcStackMapScheme=delta-main
// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi

// After the optimization pipeline every call of a function that handles Kotlin objects is a
// statepoint. The object that is live across the call is listed in gc-live, and the code after the
// call uses the relocated value.

import kotlin.native.NoInline

class A(var x: Int)

@NoInline
fun opaque() {
    println("opaque")
}

// CHECK-LABEL: define {{.*}} @"kfun:#holdAcrossCall(A){}kotlin.Int"
// CHECK-SAME: gc "kotlin-native"
// CHECK: call token {{.*}} @llvm.experimental.gc.statepoint
// CHECK-SAME: "gc-live"(ptr addrspace(1)
// CHECK: call {{.*}} ptr addrspace(1) @llvm.experimental.gc.relocate.p1(
// CHECK-NOT: @EnterFrame
@NoInline
fun holdAcrossCall(a: A): Int {
    opaque()
    return a.x
}

fun box(): String {
    if (holdAcrossCall(A(3)) != 3) return "FAIL"
    return "OK"
}
