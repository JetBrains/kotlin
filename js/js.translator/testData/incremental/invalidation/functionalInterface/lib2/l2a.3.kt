fun getString(): String {
    val s: (Any, Any, Any) -> Unit = { _, _, _ -> }
    return getTypeName(s)
}
