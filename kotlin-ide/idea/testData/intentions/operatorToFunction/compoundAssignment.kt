// IS_APPLICABLE: false
interface C {
    operator fun get(p: String): Int
    operator fun set(p: String, value: Int)
}

fun foo(c: C) {
    c<caret>[""] += 10
}