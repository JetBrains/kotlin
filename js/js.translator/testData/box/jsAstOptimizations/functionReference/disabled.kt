// CHECK_OPTIMIZED_JS

// Without data-flow optimizations the DFG-based passes do not run, so even an invoke-only
// reference keeps its K-callable wrapper.

// EXPECT_GENERATED_JS: function=invokeOnly$ref expect=disabled.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=invokeOnly$ref expect=disabled.es6.js TARGET_BACKENDS=JS_IR_ES6

fun invokeOnly(x: Int) = x + 1

fun box(): String {
    val f = ::invokeOnly
    if (f(10) != 11) return "fail invoke"
    return "OK"
}
