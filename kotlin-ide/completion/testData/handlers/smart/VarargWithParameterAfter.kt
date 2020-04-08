fun foo(vararg strings: String, p: String){ }

fun bar(s: String){
    foo(<caret>)
}

// ELEMENT: s
