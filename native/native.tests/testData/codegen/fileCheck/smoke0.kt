// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-LABEL: "kfun:#id(kotlin.Any?){}kotlin.Any?"
fun id(a: Any?): Any? {
    return a
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define ptr{{( addrspace\(1\))?}} @"kfun:#box(){}kotlin.String"
fun box(): String {
    // CHECK: call ptr{{( addrspace\(1\))?}} @"kfun:#id(kotlin.Any?){}kotlin.Any?"
    val x = id("Hello")
    // CHECK: call void @"kfun:kotlin.io#println(kotlin.Any?){}"(ptr{{( addrspace\(1\))?}} {{.*}})
    println(x)
// CHECK-LABEL: epilogue:
    return "OK"
}