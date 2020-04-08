// WITH_RUNTIME
// See KT-13411

fun use() {
    val text = B.text
    text.map { <caret>it.toString() }
}