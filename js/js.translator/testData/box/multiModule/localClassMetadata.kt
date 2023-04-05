// KJS_WITH_FULL_RUNTIME
// EXPECTED_REACHABLE_NODES: 1319
// MODULE: lib
// FILE: lib.kt

interface I {
    fun foo(): String
}

abstract class A {
    abstract fun bar(): String
}

abstract class G<T> {
    abstract fun baz(): T
}

class C {
    private val propA = object : A() {
        override fun bar() = "propA.bar"
    }

    private val propI = object : I {
        override fun foo() = "propI.foo"
    }

    private val propAI = object : A(), I {
        override fun foo() = "propAI.foo"

        override fun bar() = "propAI.bar"
    }

    private val propG = object : G<String>() {
        override fun baz() = "propG.baz"
    }

    private val propInner = object {
        inner class D {
            fun df() = "propInner.df"
        }
        fun d(): D = D()
    }.d()

    private val propL = run {
        class L {
            fun l() = "propL.l"
        }
        L()
    }

    private val propL2 = run {
        class L {
            inner class L1 {
                inner class L2 {
                    fun l2() = "propL2.l2"
                }
            }
        }

        L().L1().L2()
    }

    fun test() = "${propA.bar()};${propI.foo()};${propAI.foo()};${propAI.bar()};${propG.baz()};${propInner.df()};${propL.l()};${propL2.l2()}"
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val result = C().test()
    if (result != "propA.bar;propI.foo;propAI.foo;propAI.bar;propG.baz;propInner.df;propL.l;propL2.l2") return "fail: $result"

    return "OK"
}
