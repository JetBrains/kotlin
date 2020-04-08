fun foo(p: Int){}

fun bar() {
    foo("".hashCode<caret>())
}