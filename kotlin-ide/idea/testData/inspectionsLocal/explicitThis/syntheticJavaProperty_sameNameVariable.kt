// WITH_RUNTIME
// PROBLEM: none

fun test() {

    val isB = true

    Foo().apply {
        <caret>this.isB = true
    }
}
