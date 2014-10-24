// ERROR: This type is final, so it cannot be inherited from
package a.b

class Base {
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