fun demo(f: () -> String) = f()

// EXPECT_GENERATED_JS: function=test$lambda expect=deadCodeElimination.out.js TARGET_BACKENDS=JS_IR
// EXPECT_GENERATED_JS: function=test$lambda expect=deadCodeElimination.out.es6.js TARGET_BACKENDS=JS_IR_ES6
fun test(x: String?): String {
    val r = demo {
        val z = x ?: run {
            return@demo "OK"
        }
        "Fail 1: $z"
    }
    return r
}

fun box(): String {
    val r = test(null)
    if (r != "OK") {
        return "Fail test, got $r"
    }
    return "OK"
}
