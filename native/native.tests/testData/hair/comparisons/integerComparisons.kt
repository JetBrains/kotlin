fun lt(a: Int, b: Int): Boolean = a < b
fun le(a: Int, b: Int): Boolean = a <= b
fun gt(a: Int, b: Int): Boolean = a > b
fun ge(a: Int, b: Int): Boolean = a >= b
fun main() {
    var r: Boolean

    r = lt(1, 2); if (!r) error("lt(1, 2) = $r, expected true")
    r = lt(2, 1); if (r) error("lt(2, 1) = $r, expected false")
    r = le(2, 2); if (!r) error("le(2, 2) = $r, expected true")
    r = le(3, 2); if (r) error("le(3, 2) = $r, expected false")
    r = gt(2, 1); if (!r) error("gt(2, 1) = $r, expected true")
    r = gt(1, 2); if (r) error("gt(1, 2) = $r, expected false")
    r = ge(2, 2); if (!r) error("ge(2, 2) = $r, expected true")
    r = ge(1, 2); if (r) error("ge(1, 2) = $r, expected false")
}
