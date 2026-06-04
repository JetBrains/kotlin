fun sumTo(n: Int): Int {
    var i = 1
    var acc = 0
    while (i <= n) {
        acc = acc + i
        i = i + 1
    }
    return acc
}
fun main() {
    val r = sumTo(5)
    if (r != 15) error("sumTo(5) = $r, expected 15")
}
