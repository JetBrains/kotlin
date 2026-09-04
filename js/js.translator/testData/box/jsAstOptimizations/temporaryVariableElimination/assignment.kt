// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=test;box expect=assignment.optimized.js TARGET_BACKENDS=JS_IR_ES6

fun test(a: Int, b: Int, c: Int): Int {
    val tmp: Int
    tmp = a + b
    return tmp + c
}

fun box(): String {
    val result = test(2, 3, 4)
    if (result != 9) return "fail: " + result

    return "OK"
}
