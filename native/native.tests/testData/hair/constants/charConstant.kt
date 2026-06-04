fun letter(): Char = 'K'
fun main() {
    val r = letter()
    if (r != 'K') error("letter() = $r, expected 'K'")
}
