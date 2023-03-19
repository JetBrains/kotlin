fun getString(): String {
    val s: (Int, Int, Int, Int, Int, Int, Int, Int, Int) -> Unit = { _, _, _, _, _, _, _, _, _ -> }
    return "${s::class}"
}
