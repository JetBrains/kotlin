// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

fun flameThrower() {
    throw Throwable("🔥")
}

// CHECK-LABEL: "kfun:#f1(){}"
fun f1() {
    // CHECK: call void @"kfun:#flameThrower(){}"()
    flameThrower()
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: "kfun:#f2(){}"
fun f2() {
    try {
        // CHECK: invoke void @"kfun:#flameThrower(){}"()
        flameThrower()
    } catch (t: Throwable) {}
// CHECK-LABEL: epilogue:
}

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"
fun box(): String {
    try {
        f1()
    } catch (t: Throwable) {}
    f2()
// CHECK-LABEL: epilogue:
    return "OK"
}