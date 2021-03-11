// "Add 'kotlin.Any' as upper bound for E" "true"

fun <T : Any> foo() = 1

fun <E> bar() = foo<E<caret>>()
