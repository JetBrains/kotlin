fun reportError(): Nothing

fun usage(a: Int?): Int {
    return a ?: a ?: a ?: re<caret>
}

// ORDER: reportError
// ORDER: return
