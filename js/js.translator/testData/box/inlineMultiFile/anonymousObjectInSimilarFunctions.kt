// EXPECTED_REACHABLE_NODES: 497
// FILE: a.kt

inline fun foo(x: String): I = object : I {
    override fun get(): String = "foo_String($x)"
}

// FILE: b.kt
inline fun foo(x: Int): I = object : I {
    override fun get(): String = "foo_Int($x)"
}

// FILE: main.kt
interface I {
    fun get(): String
}

fun box(): String {
    val a = foo("1").get()
    if (a != "foo_String(1)") return "fail1: $a"

    val b = foo(2).get()
    if (b != "foo_Int(2)") return "fail2: $b"

    return "OK"
}