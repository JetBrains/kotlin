fun foo() {}

fun test() {
    <caret>for (i in 1..4) /* aaa */ foo() // bbb
}
