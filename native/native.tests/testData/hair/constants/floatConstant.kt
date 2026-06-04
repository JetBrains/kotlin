fun answer(): Float = 1.5f
fun main() {
    val r = answer()
    if (r != 1.5f) error("answer() = $r, expected 1.5f")
}
