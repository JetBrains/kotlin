class B private constructor(p: Int) {
    constructor() : this(<caret>)
    protected constructor(s: String) : this()
}

/*
Text: (<highlight>p: Int</highlight>), Disabled: false, Strikeout: false, Green: false
Text: (<highlight>s: String</highlight>), Disabled: false, Strikeout: false, Green: false
*/