class Box(var value: Int)
fun roundTrip(): Int {
    val b = Box(10)
    b.value = 42
    return b.value
}
fun main() {
    val r = roundTrip()
    if (r != 42) error("roundTrip() = $r, expected 42")
}
