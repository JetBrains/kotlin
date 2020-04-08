// IS_APPLICABLE: false
fun foo(<caret>vararg s: String): Boolean = true

fun test() {
    foo()
    foo("1")
    foo("1", "2")
}