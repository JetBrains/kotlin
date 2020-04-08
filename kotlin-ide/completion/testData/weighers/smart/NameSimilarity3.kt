fun f(x: Int, fooBar1: String, fooBar2: String){}

fun g(someBar0: String, someBar1: String, someBar2: String, fooBar: String, fooBar0: String, fooBar1: String, fooBar2: String) {
    f(1, <caret>)
}

// ORDER: fooBar1, fooBar, fooBar0, fooBar2, someBar1, someBar0, someBar2
