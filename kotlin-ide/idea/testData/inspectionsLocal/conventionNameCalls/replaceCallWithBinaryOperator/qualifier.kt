class C {
    companion object {
        operator fun plus(s: String): C = C()
    }
}

fun foo() {
    C.<caret>plus("x")
}
