// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_EVERYWHERE

class A(val x: Int)

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
fun box(): String {
    // CHECK-DEBUG: call ptr @AllocInstance
    // CHECK-OPT: alloca %"kclassbody:A#internal"
    val a = A(5)
    println(a.x)
// CHECK-LABEL: epilogue:
    return "OK"
}
