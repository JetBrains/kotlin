class A {
    operator fun get(x: Int) {}
    operator fun set(x: String, y: Int, value: Int) {}

    fun d(x: Int) {
        this[<caret>] = 1
    }
}

/*
Text: (<highlight>x: String</highlight>, y: Int), Disabled: false, Strikeout: false, Green: true
*/
