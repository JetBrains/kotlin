// "Create parameter 's'" "true"

class Foo(val n: Int) {

}

fun bar() {
    Foo(n = 1, <caret>s = "2")
}