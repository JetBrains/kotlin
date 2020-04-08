// "Create member function 'F.foo'" "true"
class F {
    fun bar() {

    }
}

class X {
    val f: Int = F().<caret>foo(1, "2")
}