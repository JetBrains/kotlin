fun foo(p: String?) {
    fun local(): String? {
        if (p == null) return null
        print(p.size)
        return ""
    }

    <caret>p?.size
}