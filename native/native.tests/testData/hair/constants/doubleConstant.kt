fun answer(): Double = 2.5
fun main() {
    val r = answer()
    if (r != 2.5) error("answer() = $r, expected 2.5")
}
