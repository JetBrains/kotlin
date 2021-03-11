fun x(name: String, next: (name: String) -> String): String {
    return next(<caret>)
}

/*
Text: (<highlight>name: String</highlight>), Disabled: false, Strikeout: false, Green: true
*/
