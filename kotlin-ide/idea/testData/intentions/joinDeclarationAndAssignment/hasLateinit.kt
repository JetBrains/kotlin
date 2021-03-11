fun foo(a: String, b: String) {
    lateinit var c: String<caret>
    c = a
    c = b
}