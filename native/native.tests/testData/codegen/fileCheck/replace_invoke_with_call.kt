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

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    try {
        f1()
    } catch (t: Throwable) {}
    f2()
// CHECK-LABEL: epilogue:
    return "OK"
}