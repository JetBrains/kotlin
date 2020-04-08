fun test() {
    foo {<caret> it.length }
}

fun foo(f: (String) -> Int) {}
//Text: (<highlight>f: (String) -> Int</highlight>), Disabled: false, Strikeout: false, Green: true