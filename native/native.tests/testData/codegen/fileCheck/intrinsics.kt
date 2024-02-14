// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs

// CHECK-LABEL: "kfun:#and(kotlin.Int;kotlin.Int){}kotlin.Int"
fun and(a: Int, b: Int): Int {
    // CHECK: and {{.*}}, {{.*}}
    return a and b
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: "kfun:#ieee754(kotlin.Float;kotlin.Float){}kotlin.Boolean"
fun ieee754(a: Float, b: Float): Boolean {
    // CHECK: fcmp oeq float {{.*}}, {{.*}}
    return a == b
// CHECK-LABEL: epilogue:
}

// CHECK-LABEL: define %struct.ObjHeader* @"kfun:#box(){}kotlin.String"
fun box(): String {
    val x = and(1, 2)
    val y = ieee754(0.0f, 1.0f)
// CHECK-LABEL: epilogue:
    return if (x == 0 && !y)
        "OK"
    else "FAIL x=$x y=$y"
}
