@JsModule("test")
external fun test(step: Int): String

fun box(step: Int): String {
    if (foo() != 77) return "Fail foo"
    if (gaz() != 99) return "Fail gaz"
    return test(step)
}
