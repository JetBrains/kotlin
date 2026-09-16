// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=box expect=innerExpressionProcessed.optimized.js TARGET_BACKENDS=JS_IR_ES6

fun box(): String {
    val a = 2
    val b = a
    val c = 2 + b

    return "OK"
}
