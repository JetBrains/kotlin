fun foo(xa: Int, xb: String, xc: Int) {}

fun bar(xb: String, xa: Int, xc: Int) {
    foo(<caret>)
}

// ORDER: xa
// ORDER: "xa, xb, xc"
// ORDER: xc
