// TARGET_BACKEND: NATIVE
// DISABLE_NATIVE: gcStackMapScheme=DELTA_MAIN
// FILECHECK_STAGE: CStubs
// FREE_COMPILER_ARGS: -opt-in=kotlin.experimental.ExperimentalNativeApi

// The default scheme (shadow stack) is not changed by the delta-main support: objects are in the
// default address space, functions do not name a GC strategy, and a function that keeps objects in
// its frame creates a frame in the shadow stack.

import kotlin.native.NoInline

class A(val x: Int)

object C {
    val x = listOf(1, 2, 3)
}

// CHECK-LABEL: define ptr @"kfun:#identity(A){}A"(ptr
// CHECK-NOT: addrspace(1)
// CHECK-NOT: kotlin-native
@NoInline
fun identity(a: A): A = a
// CHECK: {{^}}epilogue:

// CHECK-LABEL: define void @"kfun:#g(){}"()
// CHECK: EnterFrame
// CHECK-NOT: addrspace(1)
@NoInline
fun g() {
    val x = C.x
    println(x.size)
}
// CHECK: {{^}}epilogue:

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    g()
    return if (identity(A(1)).x == 1) "OK" else "FAIL"
}
