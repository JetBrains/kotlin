fun answer(): Int = 42
fun main() {
    val r = answer()
    if (r != 42) error("answer() = $r, expected 42")
}
