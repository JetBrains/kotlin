inline fun <reified T> String.xfoo(p: () -> Unit){}

fun X.test() {
    val local: () -> Unit = { }
    "a".xf<caret>
}

// ELEMENT: xfoo
// TAIL_TEXT: "(local) for String in <root>"
