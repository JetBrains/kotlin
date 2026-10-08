// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_EVERYWHERE

fun plus1(x: Int) = x + 1

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"
// CHECK-OPT-NOT: Int-box
// CHECK-OPT-NOT: Int-unbox
// CHECK-DEBUG: Int-box
// CHECK-DEBUG: Int-unbox
// CHECK-LABEL: epilogue:
fun box(): String {

    val ref = ::plus1
    var y = 0
    repeat(100000) {
        y += ref(it)  // Should be devirtualized and invoked without boxing/unboxing (`Int-box`/`Int-unbox`)
    }
    if (y != 705082704)
        return "FAIL $y != 705082704"
    return "OK"
}
