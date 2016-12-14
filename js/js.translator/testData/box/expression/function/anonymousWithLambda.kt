fun box(): String {
    val a = (fun(): String {
        val o = { "O" }
        return o() + "K"
    })
    return a()
}