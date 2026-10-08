// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_EVERYWHERE

class C<T> {
    fun foo(x: T) = x
}

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"
// CHECK-NOT: Int-box
// CHECK-OPT-NOT: Int-unbox
// CHECK-DEBUG: Int-unbox
// CHECK-LABEL: epilogue:
fun box(): String {
    val c = C<Int>()
    val fooref = c::foo
    return if (fooref(42) == 42)
        "OK"
    else
        "FAIL"
}
