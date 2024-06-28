// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

var i = 1
// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String = when(i) {
    0 -> "FAIL: 0"
    1 -> "OK"
    2 -> "FAIL: 2"
    else -> "FAIL: else"
}
// CHECK: when_case
// CHECK: when_next
// CHECK: when_case1
// CHECK: when_next2
// CHECK: when_case3
// CHECK: when_next4
// CHECK: when_exit
// CHECK: ret void
