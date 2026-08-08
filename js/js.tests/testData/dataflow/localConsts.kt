// Precision: local constants including Char vs Int distinction.

fun box(): String {
    val flag = true
    val n = 42
    val c = 'A'
    val s = "OK"
    val z: String? = null
    val u = Unit
    if (!flag) return "FAIL"
    if (n != 42) return "FAIL"
    if (c != 'A') return "FAIL"
    if (s != "OK") return "FAIL"
    if (z != null) return "FAIL"
    if (u != Unit) return "FAIL"
    return "OK"
}
