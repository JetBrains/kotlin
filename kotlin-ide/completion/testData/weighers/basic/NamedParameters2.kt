fun f(b: Boolean, tra: Int){}

fun test(tri: Boolean, trb: Int) {
    f(tr<caret>)
}

// ORDER: true
// ORDER: tri
// ORDER: "tra ="
// ORDER: trb
// ORDER: try
