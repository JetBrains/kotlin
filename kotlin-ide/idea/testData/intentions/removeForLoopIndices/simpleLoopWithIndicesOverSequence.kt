// WITH_RUNTIME
fun foo(bar: Sequence<String>) {
    for ((i<caret>,a) in bar.withIndex()) {

    }
}