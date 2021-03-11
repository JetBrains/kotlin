// "Create extension property 'String?.notExistingVal'" "true"
fun foo(n: Int) {}

fun context(p: String?) {
    foo(p.<caret>notExistingVal)
}