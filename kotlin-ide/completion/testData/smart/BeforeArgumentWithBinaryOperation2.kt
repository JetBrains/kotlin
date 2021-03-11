val v1: Int = 10
val v2: String = ""

fun foo(i: Int){}

var call: A = foo(<caret>3 + 2)
    set(value) {  }

// EXIST: v1
// ABSENT: v2
