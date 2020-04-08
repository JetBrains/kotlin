interface I

fun I.foo(p: Int): Boolean = true

fun foo(o: Any) {
    if (o is I) {
        o.foo(<caret>)
    }
}

//Text: (<highlight>p: Int</highlight>), Disabled: false, Strikeout: false, Green: true