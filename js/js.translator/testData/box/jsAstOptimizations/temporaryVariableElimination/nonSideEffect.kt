// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=foo;box expect=nonSideEffect.optimized.js TARGET_BACKENDS=JS_IR_ES6

fun foo(x: Int): Int {
    return x
}

fun box(): String {
    val tmp1 = 1
    val tmp2 = foo(2)

    val result = tmp2 + tmp1
    if (result != 3) return "fail: $result"
    
    return "OK"
}
