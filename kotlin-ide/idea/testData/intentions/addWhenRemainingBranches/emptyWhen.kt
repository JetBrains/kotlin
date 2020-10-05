// WITH_RUNTIME

enum class Entry {
    FOO, BAR, BAZ
}

fun test(e: Entry) {
    when (e) {
        <caret>
    }
}