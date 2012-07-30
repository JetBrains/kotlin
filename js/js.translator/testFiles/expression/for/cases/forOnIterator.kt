package foo

private fun iterate(iterator: Iterator<String>):String {
    for (s in iterator) {
        return s
    }

    return ""
}

fun box(): Boolean {
    return iterate(object : Iterator<String> {
        override val hasNext: Boolean
            get() = true

        override fun next() = "42"
    }) == "42";
}