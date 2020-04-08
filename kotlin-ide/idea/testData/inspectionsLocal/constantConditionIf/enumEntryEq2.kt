// PROBLEM: Condition is always 'true'
fun foo(x: Int) {}

fun bar() {
    foo(if (<caret>test.Enum.A == test.Enum.A) 1 else 2)
}