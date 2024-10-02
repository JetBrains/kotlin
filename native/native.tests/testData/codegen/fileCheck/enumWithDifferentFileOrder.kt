// FILECHECK_STAGE: CStubs

// FILE: main.kt

// CHECK-LABEL: define ptr @"kfun:#box(){}kotlin.String"
// CHECK-NOT: call ptr @"kfun:kotlin.Enum#<get-name>(){}kotlin.String"
fun box() = Base1.OK.name

// FILE: lib.kt
enum class Base1 { OK }
