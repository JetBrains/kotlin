fun countUntil(limit: Int): Int {
    var i = 0
    while (true) {
        if (i >= limit) break
        i = i + 1
    }
    return i
}
fun main() {
    val r = countUntil(3)
    if (r != 3) error("countUntil(3) = $r, expected 3")
}
