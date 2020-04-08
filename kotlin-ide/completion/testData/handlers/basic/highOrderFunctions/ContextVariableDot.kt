fun String.xfoo(p: () -> Unit): String = ""

fun X.test() {
    val local: () -> Unit = { }
    "a".xf<caret>
}

// ELEMENT: xfoo
// TAIL_TEXT: "(local) for String in <root>"
// CHAR: '.'
