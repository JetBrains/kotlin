package a.b

internal open class Base {
    internal fun foo() {
    }
}

internal class A : Base() {
    internal inner class C {
        internal fun test() {
            super@A.foo()
        }
    }
}