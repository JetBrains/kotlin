// "Create member function 'Bar.foo'" "true"
fun foo() {
    Bar.BAZ.<caret>foo()
}

enum class Bar {
    BAZ
}