open class A(x: Int) {
    fun m(x: Int, y: Boolean) = 2

    fun d(x: Int) {
        m(<caret>1, false)
    }
}
//Text: (<highlight>x: Int</highlight>, y: Boolean), Disabled: false, Strikeout: false, Green: true