// PROBLEM: none
class C

fun foo(arg: Any) {
    when (arg) {
        <caret>is C -> {
        }
    }
}