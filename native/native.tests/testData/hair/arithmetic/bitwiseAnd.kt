fun bitwiseAnd(a: Int, b: Int): Int = a and b
fun main() {
    val r = bitwiseAnd(0b1100, 0b1010)
    if (r != 0b1000) error("bitwiseAnd(0b1100, 0b1010) = $r, expected ${0b1000}")
}
