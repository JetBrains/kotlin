fun add(a: Int, b: Int): Int = a + b
fun main() {
    val r = add(2, 3)
    if (r != 5) error("add(2, 3) = $r, expected 5")
}
