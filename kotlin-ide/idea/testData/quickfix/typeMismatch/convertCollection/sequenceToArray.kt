// "Convert expression to 'Array' by inserting '.toList().toTypedArray()'" "true"
// WITH_RUNTIME

fun foo(a: Sequence<String>) {
    bar(a<caret>)
}

fun bar(a: Array<String>) {}