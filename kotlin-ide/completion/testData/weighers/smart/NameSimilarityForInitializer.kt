var fooBar = ""

class C {
    fun foo(s: String) {
        val bar: String = <caret>
    }
}

// ORDER: fooBar, s
