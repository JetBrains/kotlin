var counter: Int = 0
fun bump() { counter = counter + 1 }
fun read(): Int = counter
fun main() {
    bump()
    bump()
    bump()
    val r = read()
    if (r != 3) error("read() after 3 bumps = $r, expected 3")
}
