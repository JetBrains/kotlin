val a = true
val b = true
val c = true

fun foo() = true
fun bar() = true

// EXPECT_GENERATED_JS: function=test expect=logicalOperators.js
fun test(): Boolean = (!a || (b && c)) && foo() || bar()

fun box(): String {
    return if (test()) "OK" else "FAILED"
}
