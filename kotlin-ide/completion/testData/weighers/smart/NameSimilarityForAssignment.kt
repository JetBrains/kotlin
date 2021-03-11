var fooBar = ""

class C {
    var bar = ""

    fun foo(s: String) {
        bar = <caret>
    }
}

// ORDER: fooBar, s
