// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun main(x: Int) {
    if (x <caret>!is Int) {
        doSomething("test")
    }
}
