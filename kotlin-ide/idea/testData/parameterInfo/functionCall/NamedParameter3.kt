open class A(x: Int) {
    fun m(x: Int, y: Boolean) = 1

    fun d(x: Int) {
        m(y = false, <caret>)
    }
}
/*
Text: (<disabled>[y: Boolean],</disabled><highlight> </highlight>[x: Int]), Disabled: false, Strikeout: false, Green: true
*/