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
// FILECHECK_STAGE: RewriteStatepointsForGC
// FREE_COMPILER_ARGS: -Xbinary=gcStackMapScheme=delta-main
// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi

// After the optimization pipeline every call of a function that handles Kotlin objects is a
// statepoint. The object that is live across the call is listed in gc-live, and the code after the
// call uses the relocated value.
//
// @NoInline only stops the Kotlin/Native inliner, the LLVM inliner of the LTO pipeline can still put the
// function into its caller. @Retain keeps the function in llvm.used, so its own definition stays in the
// module and can be checked even when a copy of it is inlined into box().

@file:Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
import kotlin.native.NoInline
import kotlin.native.Retain

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
@Retain
fun holdAcrossCall(a: A): Int {
    opaque()
    return a.x
}

fun box(): String {
    if (holdAcrossCall(A(3)) != 3) return "FAIL"
    return "OK"
}
