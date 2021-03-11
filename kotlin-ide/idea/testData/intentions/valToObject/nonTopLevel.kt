// IS_APPLICABLE: false

interface B {
}

class Foo {
    val <caret>a = object : B {
    }
}