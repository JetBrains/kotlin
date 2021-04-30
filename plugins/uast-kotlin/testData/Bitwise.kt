// !IGNORE_FIR

fun foo(): Int {
    val mask: Int = 0x7f
    val x: Int = 0b1010_1010_1010_1010_1010_1010_1010_1010

    val pos = x and mask
    val max = x or mask
    val zebra = x xor mask

    val signed = x shr 2
    val one = x ushr 31
    val zero = x shl 32

    return pos + zero - zebra * signed / one
}

fun bar(): Long {
    val mask: Long = 0x7f
    val x: Long = 0x5555555555555555

    val pos = x and mask
    val max = x or mask
    val zebra = x xor mask

    val signed = x shr 2
    val one = x ushr 63
    val zero = x shl 64

    return pos + zero - zebra * signed / one
}

