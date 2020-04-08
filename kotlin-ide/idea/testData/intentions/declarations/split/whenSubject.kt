// IS_APPLICABLE: false
enum class Type {
    HYDRO,
    PYRO
}

fun select(t: Type): Int {
    return when (<caret>val i = t.ordinal) {
        0 -> 1
        1 -> 42
        else -> i
    }
}