package foo

data class D(val start: String, middle: String, val end: String) {
    fun getLabel() : String {
        return start + end
    }
}

fun box(): String {
    val d1 = D("OO", "-", "PS")
    val d2: D = d1.copy(end = "K", middle = "+")
    d2.getLabel()
    val d3: D = d2.copy(start = "O", middle = "-")
    val (p1, p2) = d3
    return p1 + p2
}