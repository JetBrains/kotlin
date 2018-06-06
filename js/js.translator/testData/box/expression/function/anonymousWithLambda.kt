// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1110
fun box(): String {
    val a = (fun(): String {
        val o = { "O" }
        return o() + "K"
    })
    return a()
}