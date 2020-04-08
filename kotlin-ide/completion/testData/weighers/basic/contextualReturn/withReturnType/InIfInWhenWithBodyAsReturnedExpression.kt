fun reportError(): Nothing

fun usage(a: Int?): Int {
    return when {
        a == null -> {
            if (true) { re<caret> }

            10
        }

        else -> a
    }
}

// ORDER: return
// ORDER: reportError
