fun foo() {
    fun fff(p: String, c: Char) {}

    fff(<caret>)
}

// TYPE: "1, "

//Text: (p: String, <highlight>c: Char</highlight>), Disabled: true, Strikeout: false, Green: true