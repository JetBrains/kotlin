fun yes(): Boolean = true
fun no(): Boolean = false
fun main() {
    if (!yes()) error("yes() = false, expected true")
    if (no()) error("no() = true, expected false")
}
