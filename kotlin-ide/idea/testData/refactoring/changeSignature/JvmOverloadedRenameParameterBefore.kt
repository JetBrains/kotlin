@JvmOverloads fun <caret>foo(a: Int, b: Int, c: Int = 1, d: Int = 2) {

}

fun test() {
    foo(a = 1, b = 2, c = 3, d = 4)
    foo(a = 1, b = 2, c = 3)
    foo(b = 1, c = 2, a = 3)
    foo(a = 1, b = 2)
    foo(b = 1, a = 2)
}