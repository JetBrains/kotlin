// EXPECTED_REACHABLE_NODES: 1251
fun box(): String {
    val a = (fun(): String {
        val o = { "O" }
        return o() + "K"
    })
    return a()
}