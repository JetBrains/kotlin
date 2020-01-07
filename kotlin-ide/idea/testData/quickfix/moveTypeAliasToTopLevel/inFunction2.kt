// "Move typealias to top level" "true"
fun bar() {
    class C {
        <caret>typealias Foo = String

        fun baz(foo: Foo) {
        }
    }
}

fun qux() {}