// PROBLEM: none
class Foo(vararg children: Foo<caret>)

fun test() {
    Foo()
}