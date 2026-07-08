fun roundTrip(): Int {
    val a = IntArray(5)
    a[2] = 42
    a[4] = a[2] + 1
    return a[2] + a[4]
}
fun main() {
    val r = roundTrip()
    if (r != 85) error("roundTrip() = $r, expected 85")
}
