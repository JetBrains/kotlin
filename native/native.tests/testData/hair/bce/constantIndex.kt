fun lastOfTen(): Int {
    val a = IntArray(10)
    a[9] = 4
    return a[9]
}

fun main() {
    val r = lastOfTen()
    if (r != 4) error("lastOfTen() = $r, expected 4")
}
