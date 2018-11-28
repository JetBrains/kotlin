// EXPECTED_REACHABLE_NODES: 1284
// IGNORE_BACKEND: JS
package foo

fun box(): String {
    val byteOne = 1.toByte()
    val shortOne = 1.toShort()
    var v: Int

    v = maxInt() + byteOne
    if (v != minInt()) return "fail1: $v"

    v = minInt() - byteOne
    if (v != maxInt()) return "fail2: $v"

    v = maxInt() + shortOne
    if (v != minInt()) return "fail3: $v"

    v = minInt() - shortOne
    if (v != maxInt()) return "fail4: $v"

    v = maxInt() * Byte.MAX_VALUE
    if (v != 2147483521) return "fail5: $v"

    v = maxInt() * Short.MAX_VALUE
    if (v != 2147450881) return "fail6: $v"

    return "OK"
}

fun minInt() = Int.MIN_VALUE
fun maxInt() = Int.MAX_VALUE