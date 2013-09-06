package foo

val a: Int = 1
    get() = $a + 1

fun box(): String {
    if (a != 2) return "a != 2, it: $a"
    return "OK"
}