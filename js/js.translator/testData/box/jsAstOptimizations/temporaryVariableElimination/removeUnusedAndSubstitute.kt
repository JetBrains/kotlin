// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=test;box expect=removeUnusedAndSubstitute.optimized.js TARGET_BACKENDS=JS_IR_ES6

var log = 0

fun test(a: Int): Int {
    val tmp1: Int
    log += if (a == 3) {
        tmp1 = 1
        tmp1
    } else {
        tmp1 = -100
        tmp1
    }
    val tmp2 = tmp1
    val tmp3: Int
    val tmp4 = tmp2
    return a
}

fun box(): String {
    if (test(3) != 3) return "fail1"
    if (log != 1) return "fail2"
    return "OK"
}
