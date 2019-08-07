// EXPECTED_REACHABLE_NODES: 1284
// IGNORE_BACKEND: JS
package foo

// TODO(WASM) Companions are not supported yet

fun box(): String {
    val byteOne = 1.toByte()
    val shortOne = 1.toShort()
    var v: Int

    v = maxInt() + byteOne
    if (v != minInt()) return "fail1"

    v = minInt() - byteOne
    if (v != maxInt()) return "fail2"

    v = maxInt() + shortOne
    if (v != minInt()) return "fail3"

    v = minInt() - shortOne
    if (v != maxInt()) return "fail4"

    v = maxInt() * 127 // Byte.MAX_VALUE
    if (v != 2147483521) return "fail5"

    v = maxInt() * 32767 // Short.MAX_VALUE
    if (v != 2147450881) return "fail6"

    return "OK"
}

fun minInt() = -2147483648 // Int.MIN_VALUE
fun maxInt() = 2147483647 // Int.MAX_VALUE