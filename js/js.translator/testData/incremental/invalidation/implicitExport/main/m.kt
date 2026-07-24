@JsModule("test")
external fun test(step: Int): String

fun box(step: Int): String {
    return test(step)
}
