fun <T1, T2> f(p: Int, t1: T1, t2: T2){}

fun test() {
    f(1, "", <caret>)
}

/*
Text: (p: Int, t1: String, <highlight>t2: T2</highlight>), Disabled: false, Strikeout: false, Green: true
*/