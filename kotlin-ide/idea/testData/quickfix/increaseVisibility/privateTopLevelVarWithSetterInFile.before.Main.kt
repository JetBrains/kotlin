// "Make '<set-prop>' internal" "true"
// ACTION: Make '<set-prop>' public
// ERROR: Cannot assign to 'prop': the setter is private in file

package test

fun foo() {
    <caret>prop = 20
}
