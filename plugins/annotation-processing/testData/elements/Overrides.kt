annotation class Anno

@Anno
open class Parent {
    open fun a() {}
}

open class Child : Parent() {
    override fun a() {}
    fun a(name: String) {}
    open fun b() = "A"
}

class ChildOfChild : Child() {
    override fun a() {}
    override fun b() = "B"
    fun a(name: CharSequence) {}
}