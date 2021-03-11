fun foo(p : suspend (String, Char) -> Boolean){}
fun foo(p : suspend (String, Boolean) -> Boolean){}

fun main(args: Array<String>) {
    fo<caret>
}

// ELEMENT: foo
// TAIL_TEXT: " { String, Char -> ... } (p: suspend (String, Char) -> Boolean) (<root>)"
