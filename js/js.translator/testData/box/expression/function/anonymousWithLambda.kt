// EXPECTED_REACHABLE_NODES: 1376
fun box(): String {
    val a = (fun(): String {
        val o = { "O" }
        return o() + "K"
    })
    return a()
}