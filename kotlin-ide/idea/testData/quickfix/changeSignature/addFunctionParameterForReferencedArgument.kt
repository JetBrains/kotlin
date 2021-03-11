// "Add parameter to function 'foo'" "true"
// DISABLE-ERRORS
fun foo() {}

fun test(isObject: Boolean) {
    foo(isObject<caret>)
}