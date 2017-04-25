// EXPECTED_REACHABLE_NODES: 489
fun box(): String {
    val a = (fun(): String {
        val o = { "O" }
        return o() + "K"
    })
    return a()
}