package foo

class A() {

    fun plus() = "hooray"
    fun minus() = "not really"

}

fun box(): Boolean {
    var c = A()
    return (+c + -c == "hooraynot really")
}