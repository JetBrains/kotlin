fun helper(x: Int): Int = x * 2
fun caller(x: Int): Int = helper(x) + 1
fun main() {
    val r = caller(5)
    if (r != 11) error("caller(5) = $r, expected 11")
}
