// EXPECTED_REACHABLE_NODES: 488
package foo

fun Int.foo() {
}
fun String.foo() {
}

val Int.bar: Int get() = 1
val String.bar: Int get() = 2

fun box(): String {
    val a = 43
    if (a.bar != 1) return "a.bar != 1, it: ${a.bar}"
    return "OK"
}