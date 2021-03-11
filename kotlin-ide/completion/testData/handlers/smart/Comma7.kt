fun foo(c: C, i: Int){ }

class C {
    fun bar() {
        foo(<caret>)
    }
}

//ELEMENT: this
