fun bitwiseOr(a: Int, b: Int): Int = a or b
fun main() {
    val r = bitwiseOr(0b1100, 0b1010)
    if (r != 0b1110) error("bitwiseOr(0b1100, 0b1010) = $r, expected ${0b1110}")
}
