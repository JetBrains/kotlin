// "Create function 'foo'" "true"

fun test(s: String?) {
    if (s == null) return
    <caret>foo(s)
}
