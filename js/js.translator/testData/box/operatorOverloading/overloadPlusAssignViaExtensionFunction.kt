// EXPECTED_REACHABLE_NODES: 891
package foo


class A<T>(val list: MutableList<T>) {
    fun addAll(c: Collection<T>) {
        list.addAll(c)
    }
}

operator fun <T> A<T>.plusAssign(other: Collection<T>) {
    addAll(other)
}

fun box(): String {
    var v1 = arrayListOf("foo")
    val v2 = listOf("bar")

    val a = A(v1)
    a += v2

    if (v1.size != 2) return "fail1: ${v1.size}"
    if (v1[0] != "foo") return "fail2: ${v1[0]}"
    if (v1[1] != "bar") return "fail3: ${v1[1]}"

    return "OK"
}
