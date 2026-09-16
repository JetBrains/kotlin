// TARGET_BACKEND: JS_IR
// CHECK_OPTIMIZED_JS
// EXPECT_GENERATED_JS: function=box expect=throughLocalAssignment.optimized.js TARGET_BACKENDS=JS_IR_ES6
// ENABLE_UNUSED_PROPERTY_DCE

fun box(): String {
    var result = ""
    val tmp: String
    tmp = result
    result += "fail"
    result = tmp + "OK"
    return result
}
