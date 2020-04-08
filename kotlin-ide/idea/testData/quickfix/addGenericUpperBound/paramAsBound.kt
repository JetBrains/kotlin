// "Add 'E' as upper bound for F" "true"

fun <T, U : T> foo() = 1

fun <E, F> bar() = foo<E, F<caret>>()
