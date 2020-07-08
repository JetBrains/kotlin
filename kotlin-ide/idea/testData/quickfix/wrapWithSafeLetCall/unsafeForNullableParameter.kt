// "Wrap with '?.let { ... }' call" "false"
// ACTION: Add 's =' to argument
// ACTION: Add non-null asserted (!!) call
// ACTION: Replace with safe (?.) call
// ACTION: Surround with null check
// DISABLE-ERRORS
// WITH_RUNTIME
fun foo(s: String?) {}

fun bar(s: String?) {
    foo(s<caret>.substring(1))
}