fun f(fooBar: String){}

fun g(b: Boolean, aaa: String, aaa1:String, foo: String, bar: String) {
    f(if (b) aaa else <caret>)
}

// ORDER: bar, foo, aaa, aaa1
