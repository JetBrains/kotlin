fun nothing(): Any? = null
fun main() {
    val r = nothing()
    if (r != null) error("nothing() = $r, expected null")
}
