// "Create label foo@" "false"
// ERROR: Unresolved reference: @foo

fun test(): Int {
    return@<caret>foo 1
}