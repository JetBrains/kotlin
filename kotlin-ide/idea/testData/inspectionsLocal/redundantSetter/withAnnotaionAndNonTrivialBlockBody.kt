// PROBLEM: none
class Foo {
    var foo: String = ""
        @Deprecated("") <caret>set(foo) {
            1 + 2
            field = foo
        }
}