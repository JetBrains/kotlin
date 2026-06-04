fun bitwiseXor(a: Int, b: Int): Int = a xor b
fun main() {
    val r = bitwiseXor(0b1100, 0b1010)
    if (r != 0b0110) error("bitwiseXor(0b1100, 0b1010) = $r, expected ${0b0110}")
}
