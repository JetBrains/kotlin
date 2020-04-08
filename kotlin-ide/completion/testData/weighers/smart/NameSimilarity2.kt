fun f(fooBar: String){}

fun g(fooA: String, someBar: String, fooBar: String, fooStuff: String) {
    f(<caret>)
}

// ORDER: fooBar, someBar, fooA, fooStuff
