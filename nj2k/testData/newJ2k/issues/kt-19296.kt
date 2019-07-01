open class A {
    interface I {
        fun f()
    }
}

class B : A()
class Test {
    var z: A.I? = null
}