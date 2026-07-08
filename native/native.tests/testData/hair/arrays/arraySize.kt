fun size(): Int = IntArray(37).size
fun main() {
    val r = size()
    if (r != 37) error("size() = $r, expected 37")
}
