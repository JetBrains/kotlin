// EXPECTED_REACHABLE_NODES: 488
package foo

fun box(): String {
    fun String.test(i: Int) = this + i + "OK"
    val a = "foo".test(32)
    if (a != "foo32OK") return "$a"

    return "OK"
}