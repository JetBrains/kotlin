fun foo(p: (java.util.Date) -> Unit){}
fun foo(p: (String) -> Unit){}

fun bar() {
    foo(<caret>)
}

// ELEMENT: "{ Date -> ... }"
