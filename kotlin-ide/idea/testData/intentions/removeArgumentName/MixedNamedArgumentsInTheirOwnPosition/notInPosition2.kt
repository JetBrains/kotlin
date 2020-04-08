// COMPILER_ARGUMENTS: +XXLanguage:+MixedNamedArgumentsInTheirOwnPosition
// IS_APPLICABLE: false
fun foo(name1: Int, name2: Int, name3: Int) {}

fun usage() {
    foo(name2 = 2, name1 = 1, <caret>name3 = 3)
}