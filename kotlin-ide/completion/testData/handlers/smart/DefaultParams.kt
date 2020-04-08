fun foo(s: String, optional: String = ""){ }

fun bar(s: String){
    foo(<caret>)
}

// ELEMENT: s
