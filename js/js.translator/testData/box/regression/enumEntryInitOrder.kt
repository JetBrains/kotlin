// WITH_STDLIB

enum class E { A, B }
val x: Any = E.A

fun box(): String {
    if (x !== E.A) return "Fail"
    return "OK"
}