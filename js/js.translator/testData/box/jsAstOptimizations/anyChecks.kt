// EXPECT_GENERATED_JS: function=test expect=anyChecks.js
fun <T: Any> test(x: Any?) = x as T

fun box(): String {
    val result = test<String>("OK")

    if (result != "OK") {
        return "Fail test, got $result"
    }

    return "OK"
}
