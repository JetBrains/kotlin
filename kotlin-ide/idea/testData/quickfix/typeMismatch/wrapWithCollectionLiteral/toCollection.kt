// "Wrap element with 'listOf()' call" "true"
// WITH_RUNTIME

fun foo(a: String) {
    bar(a<caret>)
}

fun bar(a: Collection<String>) {}
