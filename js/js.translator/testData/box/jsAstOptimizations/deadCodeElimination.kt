fun demo(f: () -> String) = f()

// EXPECT_GENERATED_JS: function=test$lambda expect=deadCodeElimination.out.js TARGET_BACKENDS=JS_IR IGNORED_MODES=GENERATE_INLINE_ANONYMOUS_FUNCTIONS
// EXPECT_GENERATED_JS: function=test$lambda expect=deadCodeElimination.out.es6.js TARGET_BACKENDS=JS_IR_ES6 IGNORED_MODES=GENERATE_INLINE_ANONYMOUS_FUNCTIONS
// EXPECT_GENERATED_JS: function=test expect=deadCodeElimination.inlineAnonymousFunctions.out.js TARGET_BACKENDS=JS_IR TARGET_MODES=GENERATE_INLINE_ANONYMOUS_FUNCTIONS
// EXPECT_GENERATED_JS: function=test expect=deadCodeElimination.inlineAnonymousFunctions.out.es6.js TARGET_BACKENDS=JS_IR_ES6 TARGET_MODES=GENERATE_INLINE_ANONYMOUS_FUNCTIONS
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
