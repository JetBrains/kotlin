// "Add non-null asserted (!!) call" "true"
interface Some

fun <T: Some?> test(t: T) {
    other(<caret>t)
}

fun other(s: Any) {}