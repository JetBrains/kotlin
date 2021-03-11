class Foo {
    var foo: String = ""
        @Deprecated("") <caret>set(x) {
            field = x
        }
}