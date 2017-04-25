// EXPECTED_REACHABLE_NODES: 504
// MODULE: m3
// FILE: m3.kt

interface M3 {
    fun foo(): Int
    val baz: Int
    val baw: Int
}

// MODULE: m2
// FILE: m2.kt

open class M2 {
    inline fun foo() = 1
    inline fun <reified T : Any> bar() = T::class
    inline val baz: Int
        get() = foo() + 1
    val baw: Int
        inline get() = foo() + 2
}

// MODULE: m1(m2, m3)
// FILE: m1.kt

class M1 : M2(), M3

// MODULE: main(m1, m2)
// FILE: main.kt

// CHECK_CONTAINS_NO_CALLS: box except=equals;getKClass

fun box(): String {
    if (M1().foo() != 1) return "fail"
    if (M1().bar<M1>().simpleName != "M1") return "fail"
    if (M1().baz != 2) return "fail"
    if (M1().baw != 3) return "fail"
    return "OK"
}