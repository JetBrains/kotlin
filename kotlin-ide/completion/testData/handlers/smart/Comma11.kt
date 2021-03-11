fun foo(p1: String, vararg p2: String){}

fun bar(p: String){
    foo(<caret>)
}

// ELEMENT: p
