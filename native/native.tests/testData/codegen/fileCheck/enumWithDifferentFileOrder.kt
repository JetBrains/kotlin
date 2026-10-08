// FILECHECK_STAGE: CStubs

// FILE: main.kt

// CHECK-EAGER_SHADOW_STACK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-LATE_SHADOW_STACK-LABEL: define ptr addrspace(1) @"kfun:#box(){}kotlin.String"
// CHECK-NOT: call {{.*}}@"kfun:kotlin.Enum#<get-name>(){}kotlin.String"
fun box() = Base1.OK.name

// FILE: lib.kt
enum class Base1 { OK }
