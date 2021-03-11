fun String.foo(c: Char){}

class C {
    fun foo(i: Int){}

    fun bar(foo: String.(String) -> Unit, p1: String, p2: Int, p3: Char) {
        "".foo(<caret>)
    }
}

// EXIST: p1
// ABSENT: p2
// EXIST: p3
