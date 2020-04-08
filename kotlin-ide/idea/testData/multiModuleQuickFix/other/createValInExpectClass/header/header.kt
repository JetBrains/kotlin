// "Create member property 'Foo.foo'" "true"

expect class Foo

fun test(f: Foo) {
    takeInt(f.<caret>foo)
}

fun takeInt(n: Int) {

}