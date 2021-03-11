fun foo(vararg xFooBars: String){}

fun g(a: String, bar: String, fooBar: String, xFooBars: Array<String>) {
    foo(<caret>)
}

// ORDER: xFooBars, fooBar, bar, a
