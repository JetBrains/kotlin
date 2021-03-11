// "Convert expression to 'MutableList' by inserting '.toMutableList()'" "true"
// WITH_RUNTIME

fun foo(a: List<String>) {
    bar(a<caret>)
}

fun bar(a: MutableList<String>) {}