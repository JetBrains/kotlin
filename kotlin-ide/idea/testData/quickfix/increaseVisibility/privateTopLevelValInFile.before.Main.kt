// "Make 'prop' internal" "true"
// ACTION: Make 'prop' public
// ERROR: Cannot access 'prop': it is private in file

package test

fun foo() {
    val x = <caret>prop
}
