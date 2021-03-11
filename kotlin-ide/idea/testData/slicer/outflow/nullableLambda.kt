// FLOW: OUT

fun String.foo(<caret>p: String) {
    val v = bar(p, true, { this })
}

fun bar(receiver: String, b: Boolean, lambda: (String.() -> String)?): String {
    return if (b) lambda!!.invoke(receiver) else ""
}
