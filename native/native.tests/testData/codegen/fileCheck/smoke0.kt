// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-LABEL: "kfun:#id(kotlin.Any?){}kotlin.Any?"
fun id(a: Any?): Any? {
    return a
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    // CHECK: call %struct.ObjHeader* @"kfun:#id(kotlin.Any?){}kotlin.Any?"
    val x = id("Hello")
    // CHECK: call void @"kfun:kotlin.io#println(kotlin.Any?){}"(%struct.ObjHeader* {{.*}})
    println(x)
// CHECK-LABEL: epilogue:
    return "OK"
}