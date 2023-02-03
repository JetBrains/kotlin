fun getString(): String {
    val s: suspend (Int, Int, Int, Int, Int, Int, Int, Int) -> Unit = { _, _, _, _, _, _, _, _ -> }
    return getTypeName(s)
}
