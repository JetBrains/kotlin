// "Add parameter to function 'foo'" "true"
// DISABLE-ERRORS
fun foo() {}

fun bar(f: (String) -> Unit) {}

fun test() {
    bar {
        foo(it<caret>)
    }
}

