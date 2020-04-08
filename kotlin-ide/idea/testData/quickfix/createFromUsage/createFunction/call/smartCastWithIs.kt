// "Create function 'foo'" "true"

fun test(o: Any) {
    if (o is String) <caret>foo(o)
}
