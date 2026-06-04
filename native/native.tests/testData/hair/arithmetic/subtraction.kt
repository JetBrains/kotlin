fun sub(a: Int, b: Int): Int = a - b
fun main() {
    val r = sub(10, 3)
    if (r != 7) error("sub(10, 3) = $r, expected 7")
}
