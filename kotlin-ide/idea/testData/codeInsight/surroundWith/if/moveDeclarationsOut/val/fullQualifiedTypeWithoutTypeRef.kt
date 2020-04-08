package test

class A {}

fun foo() {
    <caret>val a = test.A()

    a.hashCode()
}