// PROBLEM: none
enum class Foo {
    ONE,
    TWO;
    val bar = Bar(<caret>this)
}

class Bar(val foo: Foo)