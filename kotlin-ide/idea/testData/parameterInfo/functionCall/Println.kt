open class A(x: Int) {
    fun xprintln(x: String) {}
    fun xprintln() {}
    fun xprintln(x: Boolean) {}

    fun d(x: Int) {
        xprintln(<caret>)
    }
}
/*
Text: (<highlight>x: Boolean</highlight>), Disabled: false, Strikeout: false, Green: false
Text: (<highlight>x: String</highlight>), Disabled: false, Strikeout: false, Green: false
Text: (<no parameters>), Disabled: false, Strikeout: false, Green: true
*/