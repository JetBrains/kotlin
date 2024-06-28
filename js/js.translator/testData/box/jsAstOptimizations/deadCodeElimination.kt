fun demo(f: () -> String) = f()

// EXPECT_GENERATED_JS: function=test$lambda expect=deadCodeEliminationTestLambda.js
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
