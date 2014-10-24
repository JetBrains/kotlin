package a.b

open class Base {
    fun foo() {
    }
}

class A : Base() {
    inner class C {
        fun test() {
            super@A.foo()
        }
    }
}