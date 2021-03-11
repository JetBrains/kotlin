fun f(fooBar: String){}

fun g(b: Boolean, a: String, foo: String, bar: String) {
    f(if (b) <caret>)
}

// ORDER: bar, foo, a
