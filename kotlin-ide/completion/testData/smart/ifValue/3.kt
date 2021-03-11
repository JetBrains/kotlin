fun foo(s: String){}
fun foo(c: Char){}

fun bar(b: Boolean, s: String, c: Char){
    foo(if (b) xxx else <caret>)
}

// EXIST: s
// EXIST: c
// ABSENT: b
