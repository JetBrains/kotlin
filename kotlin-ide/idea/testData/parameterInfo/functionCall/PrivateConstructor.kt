class A private constructor(f: Boolean) {
    constructor(): this(true)
}

fun test() {
    val a = A(<caret>)
}

//Text: (<no parameters>), Disabled: false, Strikeout: false, Green: true