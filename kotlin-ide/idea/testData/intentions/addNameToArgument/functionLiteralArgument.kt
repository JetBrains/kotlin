// IS_APPLICABLE: false
fun foo(s: String, handler: () -> Unit){}

fun bar() {
    foo("") <caret>{ }
}