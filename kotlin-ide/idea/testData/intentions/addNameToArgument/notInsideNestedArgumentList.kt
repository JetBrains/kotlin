// IS_APPLICABLE: false
fun foo(p: Int){}

fun bar() {
    foo("".hashCode(<caret>))
}