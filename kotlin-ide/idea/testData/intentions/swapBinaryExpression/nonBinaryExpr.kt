// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun main() {
    val c = 500
    doSomething(<caret>"Today is Friday")
}
