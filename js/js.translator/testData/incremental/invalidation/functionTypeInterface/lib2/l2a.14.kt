fun getString(): String {
    val s1: (Int, Int, Int, Int, Int, Int, Int, Int, Int) -> Unit = { _, _, _, _, _, _, _, _, _ -> }
    val s2: (Int, Int, Int, Int) -> Unit = { _, _, _, _ -> }
    return "${s1::class == s2::class}"
}
