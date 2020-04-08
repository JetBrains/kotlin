// "Wrap element with 'arrayOf()' call" "true"
// WITH_RUNTIME

fun foo(a: String) {
    bar(a<caret>)
}

fun bar(a: Array<String>) {}
