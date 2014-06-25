package a.b

class Base {
    fun foo() {
    }
}

class A : Base() {
    inner class C {
        fun test() {
            this@A.foo()
        }
    }
}