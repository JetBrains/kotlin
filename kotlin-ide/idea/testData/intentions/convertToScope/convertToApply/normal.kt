// WITH_RUNTIME

class MyClass {
    fun foo1() = Unit
    fun foo2() = Unit
    fun foo3() = Unit

    fun foo4() {
        val a = MyClass()
        a.foo1()<caret>
        a.foo2()
        a.foo3()
    }
}