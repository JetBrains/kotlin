package lib

enum class E {
    A, B
}

inline fun value(x: E) = when (x) {
    E.A -> "OK"
    E.B -> "Fail"
}
