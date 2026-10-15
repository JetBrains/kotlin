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

// With the delta-main scheme the stack maps replace the shadow stack: a function does not create a
// frame in the shadow stack, and the garbage collector sees its roots through the statepoints.

import kotlin.native.NoInline

object C {
    val x = listOf(1, 2, 3)
}

// The same function has a frame in the default scheme (see no_frame_on_constant_object_access.kt).
// CHECK-LABEL: define void @"kfun:#g(){}"()
// CHECK-NOT: EnterFrame
// CHECK-NOT: LeaveFrame
// CHECK-NOT: SetCurrentFrame
@NoInline
fun g() {
    val x = C.x
    println(x.size)
}
// CHECK: {{^}}epilogue:

// CHECK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"
fun box(): String {
    g()
    return "OK"
}
