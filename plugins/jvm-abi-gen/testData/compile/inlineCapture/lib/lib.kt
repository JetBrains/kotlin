package lib

fun inlineCapture(s: String): String {
    return with(StringBuilder()) {
        val o = object {
            override fun toString() = s
        }
        append(o)
    }.toString()
}
