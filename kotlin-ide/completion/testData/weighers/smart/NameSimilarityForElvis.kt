fun f(fooBar: String){}

object C {
    fun aaa(): String = ""
}

fun g(b: Boolean, aaa: String, foo: String, bar: String) {
    f(C.aaa() ?: <caret>)
}

// ORDER: bar, foo, aaa
