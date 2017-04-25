// EXPECTED_REACHABLE_NODES: 489
package foo

fun bigValue() = 0x7FFFFFFC

fun mediumValue() = 0x12345

fun box(): String {
    var v = bigValue()
    v += 1
    if (v != 0x7FFFFFFD) return "fail1: $v"

    v = bigValue()
    v += 8
    if (v != -0x7FFFFFFC) return "fail2: $v"

    v = mediumValue()
    v *= 0x23456
    if (v != -2112496338) return "fail3: $v"

    v = bigValue()
    v *= bigValue()
    if (v != 16) return "fail4: $v"

    return "OK"
}