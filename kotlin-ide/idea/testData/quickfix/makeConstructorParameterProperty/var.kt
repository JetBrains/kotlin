// "Make constructor parameter a property" "true"

class A(foo: String) {
    fun bar() {
        foo<caret> = ""
    }
}