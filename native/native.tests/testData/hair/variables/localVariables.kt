fun compute(x: Int): Int {
    var acc = x
    acc = acc + 1
    val doubled = acc + acc
    return doubled
}
fun main() {
    val r = compute(10)
    if (r != 22) error("compute(10) = $r, expected 22")
}
