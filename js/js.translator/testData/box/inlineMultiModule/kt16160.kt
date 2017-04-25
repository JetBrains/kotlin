// EXPECTED_REACHABLE_NODES: 504
// MODULE: main(module2)
// FILE: main.kt

// CHECK_CONTAINS_NO_CALLS: box except=foo;bar;toString

import A.test

fun box(): String {
    if (A.test() != 3) return "A.test()" + A.test()
    if (B.test() != 6) return "B.test()" + B.test()
    if (B().foo() != 4) return "B().foo()" + B().foo()
    if (test() != 3) return "[A.]test()" + test()
    if (test2() != 3) return "test2()" + test2()
    if (A.test2() != 3) return "A.test2()" + A.test2()
    if (B.test2() != 2) return "B.test2()" + B.test2()
    if (B.C.test() != 4) return "B.C.test()" + B.C.test()
    if (D().foo2() != 4) return "D().foo2()" + D().foo2()
    if (D.test() != 4) return "D.test()" + D.test()
    return "OK"
}

// MODULE: module2
// FILE: module2.kt

import A.foo
import B.Companion.bar

object A {
    fun foo() = 1
    inline fun test() = foo() + this.foo() + A.foo()
}

open class B {
    companion object {
        fun bar() = 2
        inline fun test() = bar() + this.bar() + B.bar()
    }

    class C {
        companion object {
            inline fun test() = bar() + B.bar()
        }
    }

    inline fun foo() = bar() + B.bar()
}

class D: B() {
    inline fun foo2() = bar() + B.bar()

    companion object {
        inline fun test() = bar() + B.bar()
    }
}

inline fun test2() = foo() + bar()
inline fun A.test2() = foo() + B.bar()
inline fun B.Companion.test2() = bar()