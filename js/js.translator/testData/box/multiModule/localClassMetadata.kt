// EXPECTED_REACHABLE_NODES: 526
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

    fun test() = "${propA.bar()};${propI.foo()};${propAI.foo()};${propAI.bar()};${propG.baz()}"
}

// MODULE: main(lib)
// FILE: main.kt
fun box(): String {
    val result = C().test()
    if (result != "propA.bar;propI.foo;propAI.foo;propAI.bar;propG.baz") return "fail: $result"

    return "OK"
}