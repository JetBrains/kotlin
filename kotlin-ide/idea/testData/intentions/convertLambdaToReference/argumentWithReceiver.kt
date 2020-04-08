// IS_APPLICABLE: false

fun callMe(s: String) {
}

fun body(receiver: String.(String) -> Unit) {
}

fun usage() {
    body { <caret>callMe(it) }
}