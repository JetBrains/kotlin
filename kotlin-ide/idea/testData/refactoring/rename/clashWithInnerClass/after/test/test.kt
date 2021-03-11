package test

class A {
    inner class C {
        inner class C {
            val c: A.C = this@A.C()
        }
    }
}