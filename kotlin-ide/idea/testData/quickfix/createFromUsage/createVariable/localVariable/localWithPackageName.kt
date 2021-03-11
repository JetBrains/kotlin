// "Create local variable 'foo'" "true"
// ACTION: Create parameter 'foo'

package foo

fun test(): Int {
    return <caret>foo
}