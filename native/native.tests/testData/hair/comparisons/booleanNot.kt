fun negate(b: Boolean): Boolean = !b
fun main() {
    var r: Boolean

    r = negate(true);  if (r)  error("negate(true) = $r, expected false")
    r = negate(false); if (!r) error("negate(false) = $r, expected true")
}
