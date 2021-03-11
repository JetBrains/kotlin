// "Replace with 'NewClass(p + 1)'" "true"
package ppp

@Deprecated("", ReplaceWith("NewClass(p + 1)"))
class OldClass(p: Int)

class NewClass(p: Int)

fun foo() {
    <caret>OldClass(1)
}
