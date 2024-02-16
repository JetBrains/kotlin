// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

class A(val x: Int)

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    // CHECK-DEBUG: call %struct.ObjHeader* @AllocInstance
    // CHECK-OPT: alloca %"kclassbody:A#internal"
    val a = A(5)
    println(a.x)
// CHECK-LABEL: epilogue:
    return "OK"
}