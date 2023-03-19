fun getString(): String {
    val s: suspend (Any, Any, Any, Any, Any) -> Unit = { _, _, _, _, _ -> }
    return "${s::class}"
}
