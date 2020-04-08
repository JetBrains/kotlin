class C {
    val something: String = ""
    val s: String = ""

    fun calcSomething(c: C?): String {
        if (c != null) {
            return c.<caret>
        }
    }
}

// ORDER: calcSomething
// ORDER: something
// ORDER: s
