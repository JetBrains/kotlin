// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtProperty
// OPTIONS: usages
// FIR_COMPARISON

fun test() {
    do {
        val <caret>message = "test"
        println(message)
    } while (message.isEmpty())
}