// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

object A {
    const val x = 5
}

class B(val z:Int) {
    companion object {
        const val y = 7
    }
}

object C {
    val x = listOf(1, 2, 3)
}

// CHECK-LABEL: define i32 @"kfun:#f(){}kotlin.Int"()
// CHECK-NOT: EnterFrame
fun f() = A.x + B.y
// CHECK: {{^}}epilogue:

// test that assumption on how EnterFrame looks like is not broken
// CHECK-LABEL: define void @"kfun:#g(){}"()
// CHECK: EnterFrame
fun g() {
    val x = C.x
}
// CHECK: {{^}}epilogue:


// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    val f = f()
    if (f != 12)
        return "FAIL: $f != 12"
    g()
    return "OK"
}