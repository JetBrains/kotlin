// COMPILER_ARGUMENTS: -XXLanguage:-NewInference

class A {
    operator fun get(x: Int) {}
    operator fun set(x: String, value: Int) {}

    fun d(x: Int) {
        this["", 1<caret>] = 1
    }
}

/*
Text: (x: String), Disabled: true, Strikeout: false, Green: true
*/
