// "Replace with 'java.util.Random'" "true"

@Deprecated("", ReplaceWith("java.util.Random"))
class OldClass

fun foo() {
    <caret>OldClass(1)
}
