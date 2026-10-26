// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_ONLY_DIST
// IGNORE_NATIVE: optimizationMode=OPT && cacheMode=STATIC_EVERYWHERE

class A(val x: Int)

// CHECK-LABEL: define ptr{{( addrspace\(1\))?}} @"kfun:#box(){}kotlin.String"
fun box(): String {
    // CHECK-DEBUG: call ptr{{( addrspace\(1\))?}} @AllocInstance
    // Escape analysis is turned off for the delta-main scheme, nothing is allocated on the stack.
    // CHECK-SHADOW_STACK-OPT: alloca %"kclassbody:A#internal"
    // CHECK-DELTA_MAIN-OPT: call ptr{{( addrspace\(1\))?}} @AllocInstance
    val a = A(5)
    println(a.x)
// CHECK-LABEL: epilogue:
    return "OK"
}
