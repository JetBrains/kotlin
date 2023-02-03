fun getString(): String {
    val s: suspend (Any) -> Unit = { _ -> }
    return getTypeName(s)
}
