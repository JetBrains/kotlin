// WITH_RUNTIME
// FIX: Introduce anonymous parameter

fun test(list: List<String>) {
    list.for<caret>Each {}
}