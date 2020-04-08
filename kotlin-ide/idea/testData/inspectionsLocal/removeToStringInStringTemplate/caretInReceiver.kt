// PROBLEM: none

fun foo(a: Int, b: Int) = a + b

fun test(): String {
    return "Foo: ${<caret>foo(0, 4).toString()}"
}