// "Create class 'Foo'" "true"

fun test() {
    val a = <caret>Foo { p: Int -> p + 1 }
}