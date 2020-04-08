// COMPILER_ARGUMENTS: +XXLanguage:+MixedNamedArgumentsInTheirOwnPosition
// IS_APPLICABLE: false
fun foo(name1: Int, name2: Int, name3: Int) {}

fun usage() {
    foo(name2 = 2, <caret>name1 = 1, name3 = 3)
}
