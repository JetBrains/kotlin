class C {
    companion object {
        operator fun get(s: String): C = C()
    }
}

fun foo() {
    C.<caret>get("x")
}
