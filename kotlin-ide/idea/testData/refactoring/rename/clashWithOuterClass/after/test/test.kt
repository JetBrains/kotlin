package test

class A {
    inner class A {
        inner class C {
            val a: A = A()
        }
    }
}