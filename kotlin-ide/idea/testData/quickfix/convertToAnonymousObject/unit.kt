// "Convert to anonymous object" "true"
interface I {
    fun bar(): Unit
}

fun foo() {
}

fun test() {
    <caret>I {
        foo()
    }
}