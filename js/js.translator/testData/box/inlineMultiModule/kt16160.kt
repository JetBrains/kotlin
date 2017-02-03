// MODULE: main(module2)
// FILE: main.kt

import A.test

fun box(): String {
    if (A.test() != 3) return "A.test()" + A.test()
    if (B.test() != 6) return "B.test()" + B.test()
    if (test() != 3) return "[A.]test()" + test()
    if (test2() != 3) return "test2()" + test2()
    if (A.test2() != 3) return "A.test2()" + A.test2()
    if (B.test2() != 2) return "B.test2()" + B.test2()
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

class B {
    companion object {
        fun bar() = 2
        inline fun test() = bar() + this.bar() + B.bar()
    }
}

inline fun test2() = foo() + bar()
inline fun A.test2() = foo() + B.bar()
inline fun B.Companion.test2() = bar()