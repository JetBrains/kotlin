// WITH_RUNTIME
// IS_APPLICABLE: false
// ERROR: 'infix' modifier is inapplicable on this function: must be a member or an extension function

package demo

infix fun foo(str: String) = kotlin.io.println(str)

fun main() {
    <caret>demo.foo("")
}
