// "Add parameter to constructor 'Foo'" "true"
// DISABLE-ERRORS
enum class Foo {
    A("A"),
    B("B"<caret>)
}