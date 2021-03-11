// IS_APPLICABLE: false
fun <T> doSomething(a: T) {}

fun main() {
    for (elt<caret> in 0..3) {
        doSomething("Hello")
    }
}
