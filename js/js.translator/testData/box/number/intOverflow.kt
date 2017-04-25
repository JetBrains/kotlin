// EXPECTED_REACHABLE_NODES: 490
package foo

fun bigValue() = 0x7FFFFFFC

fun mediumValue() = 0x12345

fun four() = 4

fun box(): String {
    var v = bigValue() + 1
    if (v != 0x7FFFFFFD) return "fail1: $v"

    v = bigValue() + 8
    if (v != -0x7FFFFFFC) return "fail2: $v"

    v = bigValue() + four() + 4
    if (v != -0x7FFFFFFC) return "fail3: $v"

    v = (bigValue() + four() - 4) shr 1
    if (v != 0x3FFFFFFE) return "fail3: $v"

    v = mediumValue() * 0x23456
    if (v != -2112496338) return "fail5: $v"

    v = bigValue() * bigValue()
    if (v != 16) return "fail6: $v"

    return "OK"
}