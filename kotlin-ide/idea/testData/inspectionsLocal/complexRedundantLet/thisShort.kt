// PROBLEM: none
// WITH_RUNTIME


fun Int.foo(): Int {
    return <caret>let { it.hashCode() }
}