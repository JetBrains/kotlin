// MODULE: m3
// FILE: m3.kt

interface M3 {
    fun foo(): Int
}

// MODULE: m2
// FILE: m2.kt

open class M2 {
    inline fun foo() = 1
    inline fun <reified T : Any> bar() = T::class
}

// MODULE: m1(m2, m3)
// FILE: m1.kt

class M1 : M2(), M3

// MODULE: main(m1, m2)
// FILE: main.kt

fun box(): String {
    if (M1().foo() != 1) return "fail"
    if (M1().bar<M1>().simpleName != "M1") return "fail"
    return "OK"
}