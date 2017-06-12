// EXPECTED_REACHABLE_NODES: 495
package foo

fun box(): String {
    val s = Array<String>(3) { it.toString() }
    if (s.size != 3) return "Fail Array size: ${s.size}"
    if (s[1] != "1") return "Fail Array value: ${s[1]}"

    val i = IntArray(3) { it }
    if (i.size != 3) return "Fail IntArray size: ${i.size}"
    if (i[1] != 1) return "Fail IntArray value: ${i[1]}"

    val c = CharArray(3) { it.toChar() }
    if (c.size != 3) return "Fail CharArray size: ${c.size}"
    if (c[1] != 1.toChar()) return "Fail CharArray value: ${c[1]}"

    val b = BooleanArray(3) { true }
    if (b.size != 3) return "Fail BooleanArray size: ${b.size}"
    if (b[1] != true) return "Fail BooleanArray value: ${b[1]}"

    val l = LongArray(3) { it.toLong() }
    if (l.size != 3) return "Fail LongArray size: ${l.size}"
    if (l[1] != 1L) return "Fail LongArray value: ${l[1]}"

    return "OK"
}
