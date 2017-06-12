// EXPECTED_REACHABLE_NODES: 497
package foo

data class Dat(val start: String, val middle: String, val end: String)

fun box(): String {
    val d1 = Dat("OO", "-", "PS")
    val d2: Dat = d1.copy(end = "K", middle = "+")
    val d3: Dat = d2.copy(start = "O", middle = "-")
    val (p1, p, p2) = d3
    return p1 + p2
}
