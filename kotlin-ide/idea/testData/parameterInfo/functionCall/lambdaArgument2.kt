fun test() {
    foo({ 1 }, { 2 }) {<caret> it.length }
}

fun foo(a: (Unit) -> Int, b: (Unit) -> Int, c: (String) -> Int) {}
//Text: (a: (Unit) -> Int, b: (Unit) -> Int, <highlight>c: (String) -> Int</highlight>), Disabled: false, Strikeout: false, Green: true