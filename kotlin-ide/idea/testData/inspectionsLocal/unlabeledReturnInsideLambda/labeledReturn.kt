// PROBLEM: none
inline fun foo(f: () -> Unit) {}

fun test(): Int {
    foo {
        <caret>return@test 0
    }
    return 1
}
