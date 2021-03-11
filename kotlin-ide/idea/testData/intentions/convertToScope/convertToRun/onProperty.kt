// IS_APPLICABLE: false

class MyClass {
    fun foo1() = Unit
    fun foo2() = Unit
    fun foo3() = Unit

    fun foo4() {
        <caret>val a = MyClass()
        a.foo1()
        a.foo2()
        a.foo3()
    }
}