// "Create class 'Foo'" "true"

fun <T> run(f: () -> T) = f()

fun test() {
    run { <caret>Foo() }
}