class C {
    companion object {
        val INSTANCE = C()
        fun create() = C()
    }
}

fun foo(p: C) {
    foo(<caret>)
}


// ORDER: p
// ORDER: C
// ORDER: INSTANCE
// ORDER: create
