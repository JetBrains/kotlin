fun foo(s: String, i: Int){}

fun bar(b: Boolean, s: String){
    foo(if (b)
            "abc"
        else {
            println()
            <caret>
    })
}

// ELEMENT: s
