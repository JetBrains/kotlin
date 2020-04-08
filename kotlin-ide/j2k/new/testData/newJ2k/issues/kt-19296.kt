import A.I

open class A {
    interface I {
        fun f()
    }
}

class B : A()
class Test {
    var z: I? = null
}