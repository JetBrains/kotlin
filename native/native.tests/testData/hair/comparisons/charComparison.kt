fun before(a: Char, b: Char): Boolean = a < b
fun main() {
    var r: Boolean

    r = before('a', 'b'); if (!r) error("before('a', 'b') = $r, expected true")
    r = before('b', 'a'); if (r) error("before('b', 'a') = $r, expected false")
}
