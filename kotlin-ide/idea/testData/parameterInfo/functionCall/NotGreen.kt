open class A(x: Int) {
    fun m(x: Int, y: Boolean) = 2
    fun m(x: Int) = 1

    fun d(x: Int) {
        m(<caret>1, false, false)
    }
}
/*
Text: (<highlight>x: Int</highlight>), Disabled: false, Strikeout: false, Green: false
Text: (<highlight>x: Int</highlight>, y: Boolean), Disabled: false, Strikeout: false, Green: false
*/