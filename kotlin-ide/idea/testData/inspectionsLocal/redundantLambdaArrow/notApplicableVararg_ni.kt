// COMPILER_ARGUMENTS: -XXLanguage:+NewInference
// PROBLEM: Redundant lambda arrow
// WITH_RUNTIME

fun main() {
    registerHandler(handlers = *arrayOf(
        { _<caret> -> },
        { it -> }
    ))
}

fun registerHandler(vararg handlers: (String) -> Unit) {
    handlers.forEach { it.invoke("hello") }
}