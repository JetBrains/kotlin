class Marker
fun same(a: Any, b: Any): Boolean = a === b
fun main() {
    val m = Marker()
    val n = Marker()
    if (!same(m, m)) error("same(m, m) = false, expected true")
    if (same(m, n)) error("same(m, n) = true, expected false")
}
