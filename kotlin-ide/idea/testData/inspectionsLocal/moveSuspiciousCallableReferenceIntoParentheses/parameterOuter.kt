// FIX: Move suspicious callable reference into parentheses '()'
// WITH_RUNTIME

fun foo(bar: Int) {
    listOf(1,2,3).map {<caret> bar::plus }
}