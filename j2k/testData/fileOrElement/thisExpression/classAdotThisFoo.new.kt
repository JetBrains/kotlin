package a.b

internal open class Base {
    open fun foo() {}
}

internal class A : Base() {
    internal inner class C {
        fun test() {
            foo()
        }
    }
}