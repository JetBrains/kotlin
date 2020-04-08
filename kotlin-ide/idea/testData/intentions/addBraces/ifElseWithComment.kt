fun foo() {}

fun test(b: Boolean) {
    <caret>if (b) foo() /* if comment */ else foo() // else comment
}
