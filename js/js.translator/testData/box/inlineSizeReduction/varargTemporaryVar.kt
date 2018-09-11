// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1283
// CHECK_VARS_COUNT: function=test1 count=0
// CHECK_VARS_COUNT: function=test2 count=0
// CHECK_VARS_COUNT: function=test3 count=1

inline fun foo(vararg x: String) = x.size

inline fun bar(vararg x: String) = x.size + x.size

fun test1() = foo("Q", "W", "E")

fun test2() = foo("*", "@")

fun test3() = bar("*", "@")

fun box(): String {
    if (test1() != 3) return "fail1: ${test1()}"
    if (test2() != 2) return "fail1: ${test2()}"
    if (test3() != 4) return "fail1: ${test3()}"

    return "OK"
}