// EXPECTED_REACHABLE_NODES: 1282
package foo


fun box(): String {
    var b: Byte = 0x7F
    b++
    if (b.toInt() != -0x80) return "fail1a"
    b--
    if (b.toInt() != 0x7F) return "fail1b"

    var s: Short = 0x7FFF
    s++
    if (s.toInt() != -0x8000) return "fail2a"
    s--
    if (s.toInt() != 0x7FFF) return "fail2b"

    var i: Int = 0x7FFFFFFF
    i++
    if (i != -0x80000000) return "fail3a"
    i--
    if (i != 0x7FFFFFFF) return "fail3b"

    return "OK"
}