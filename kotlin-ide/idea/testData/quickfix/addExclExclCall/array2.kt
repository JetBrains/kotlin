// "Add non-null asserted (!!) call" "true"

fun foo(a: Array<String?>): String {
    return <caret>a[0]
}