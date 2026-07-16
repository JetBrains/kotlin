// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_EVERYWHERE

class C {
    fun foo(x: Int) = x
}

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-OPT-NOT: Int-box
// CHECK-DEBUG: Int-box
// CHECK-NOT: Int-unbox
// CHECK-LABEL: epilogue:
fun box(): String {
    val c = C()
    val fooref = c::foo
    val result = fooref(42)
    return if( result == 42)
        "OK"
    else
        "FAIL: $result"
}
