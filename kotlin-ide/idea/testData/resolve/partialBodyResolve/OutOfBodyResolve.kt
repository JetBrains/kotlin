val v = ""

fun foo(s: String = <caret>v) {
    val local = 1
    print("foo" + local)
}