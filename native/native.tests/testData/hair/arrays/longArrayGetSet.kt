fun roundTrip(): Long {
    val a = LongArray(3)
    a[0] = 10_000_000_000L
    a[2] = a[0] * 2
    return a[0] + a[2]
}
fun main() {
    val r = roundTrip()
    if (r != 30_000_000_000L) error("roundTrip() = $r, expected 30000000000")
}
