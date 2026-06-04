fun roundTrip(x: Int): Int {
    var y = x
    y++
    y++
    y--
    return y
}
fun main() {
    val r = roundTrip(5)
    if (r != 6) error("roundTrip(5) = $r, expected 6")
}
