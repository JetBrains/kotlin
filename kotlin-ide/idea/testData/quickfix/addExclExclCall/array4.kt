// "Add non-null asserted (!!) call" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION
// ERROR: Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type Array<String?>?

fun foo(a: Array<String?>?): String {
    return <caret>a[0]
}