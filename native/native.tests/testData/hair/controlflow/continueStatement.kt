fun sumFrom(n: Int, threshold: Int): Int {
    var i = 0
    var acc = 0
    while (i < n) {
        i = i + 1
        if (i < threshold) continue
        acc = acc + i
    }
    return acc
}
fun main() {
    val r = sumFrom(5, 3)
    if (r != 12) error("sumFrom(5, 3) = $r, expected 12")
}
