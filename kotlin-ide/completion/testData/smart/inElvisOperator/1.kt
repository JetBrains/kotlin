fun foo(p: String?){}
fun foo(p: Char){}

fun bar(p1: String?, p2: String?, p3: Char) {
    foo(p1 ?: <caret>
}

// EXIST: { itemText:"p2" }
// ABSENT: p3
