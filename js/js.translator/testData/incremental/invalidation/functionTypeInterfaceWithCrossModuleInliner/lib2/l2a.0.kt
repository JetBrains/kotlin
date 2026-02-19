fun getString(): String {
    val s: suspend () -> Unit = {}
    return getTypeName(s)
}
