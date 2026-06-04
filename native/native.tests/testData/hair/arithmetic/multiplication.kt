fun mul(a: Int, b: Int): Int = a * b
fun main() {
    val r = mul(6, 7)
    if (r != 42) error("mul(6, 7) = $r, expected 42")
}
