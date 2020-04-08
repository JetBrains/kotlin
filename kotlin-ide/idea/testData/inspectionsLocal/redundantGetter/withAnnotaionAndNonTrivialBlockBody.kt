// PROBLEM: none
class Foo {
    val foo: String = ""
        @Deprecated("") <caret>get() {
            1 + 2
            return field
        }
}