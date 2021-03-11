// "Create class 'Foo'" "true"
interface I

fun <T : I> foo() {}

fun x() {
    foo<<caret>Foo>()
}