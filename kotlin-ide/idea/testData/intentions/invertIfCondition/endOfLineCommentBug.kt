fun foo(p: String?): Int? {
    <caret>if (p == null) return null
    return p.hashCode() // comment
}