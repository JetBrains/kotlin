package foo

open class A() {
    open fun f() = 3;
}

open class C() : B() {
    override fun f() = 5
}