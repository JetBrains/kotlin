fun foo(@Suppress("UNCHECKED_CAST") p: () -> Unit){}

fun bar() {
    foo(<caret>)
}
//Text: (<highlight>@Suppress p: () -> Unit</highlight>), Disabled: false, Strikeout: false, Green: true