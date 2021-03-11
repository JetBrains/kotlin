fun foo(s: String, c: Char){ }

fun takeString(p: Int): String? = null

fun bar() {
    foo(<caret>qqq(1, 2, 3))
}

// ELEMENT_TEXT: "!! takeString"
// CHAR: \t
