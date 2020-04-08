// "Create parameter 's'" "true"

fun foo(n: Int) {

}

fun bar() {
    foo(n = 1, <caret>s = "2")
}