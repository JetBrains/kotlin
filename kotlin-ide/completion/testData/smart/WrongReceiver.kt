fun String.foo(a: Int){}

class C {
    fun foo(s: String){}

    fun bar(p1: String, p2: Int) {
        "".foo(<caret>)
    }
}

// ABSENT: p1
// EXIST: p2
