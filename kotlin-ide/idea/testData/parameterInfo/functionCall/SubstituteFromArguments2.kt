fun <T1, T2> f(p: Int, t1: T1, t2: T2){}

fun test() {
    f(<caret>1, "", 1)
}

/*
Text: (<highlight>p: Int</highlight>, t1: String, t2: Int), Disabled: false, Strikeout: false, Green: true
*/