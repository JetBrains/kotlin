// TARGET_BACKEND: NATIVE
// FILECHECK_STAGE: CStubs
// ASSERTIONS_MODE: always-disable
// WITH_STDLIB

@file:Suppress("OPT_IN_USAGE_ERROR")

// CHECK-LABEL: define i32 @"kfun:#nonEmptySize(kotlin.IntArray){}kotlin.Int"
// CHECK-NOT: call void @"kfun:kotlin.AssertionError#<init>(kotlin.Any?){}"
fun nonEmptySize(x: IntArray): Int {
    assert(x.size != 0) { "x.size = ${x.size}" }
    return x.size
}

fun box(): String {
    nonEmptySize(intArrayOf(1))
    return "OK"
}