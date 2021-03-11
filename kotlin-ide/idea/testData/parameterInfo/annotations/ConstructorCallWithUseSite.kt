// WITH_RUNTIME

annotation class Fancy

class Foo(@get:Fancy val foo: Int, @param:Fancy val foo1: Int, @set:Fancy val foo2: Int)

fun bar() {
    Foo(<caret>)
}

/*
Text: (<highlight>foo: Int</highlight>, @Fancy foo1: Int, foo2: Int), Disabled: false, Strikeout: false, Green: true
*/