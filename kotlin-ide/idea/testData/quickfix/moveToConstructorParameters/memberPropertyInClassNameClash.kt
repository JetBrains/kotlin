// "Move to constructor parameters" "true"
// SHOULD_FAIL_WITH: Duplicating parameter 'n'
open class A(n: Int) {
    <caret>val n: Int
}

fun test() {
    val a = A(0)
}