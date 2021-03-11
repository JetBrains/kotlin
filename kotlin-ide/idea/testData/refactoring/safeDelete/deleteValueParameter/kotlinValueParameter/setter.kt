class A {
    var foo: String
        get() = "foo"
        set(<caret>value: String) {
            println()
        }
}

class B {
    fun bar(a: A) {
        a.foo = "bar"
    }
}