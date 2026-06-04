fun countDown(n: Int): Int {
    var i = n
    var steps = 0
    do {
        steps = steps + 1
        i = i - 1
    } while (i > 0)
    return steps
}
fun main() {
    val r = countDown(4)
    if (r != 4) error("countDown(4) = $r, expected 4")
}
