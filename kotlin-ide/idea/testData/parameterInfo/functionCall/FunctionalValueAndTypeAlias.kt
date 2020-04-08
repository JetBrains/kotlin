typealias Handler = (name: String) -> String

fun x(handler: Handler): String {
    return handler(<caret>)
}

/*
Text: (<highlight>name: String</highlight>), Disabled: false, Strikeout: false, Green: true
*/
