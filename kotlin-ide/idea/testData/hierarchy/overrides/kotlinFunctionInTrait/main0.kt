interface T {
    fun <caret>foo()
}

open class X: T {
    override fun foo() {

    }
}

open class Y: B() {
    override fun foo() {

    }
}

open class Z: X() {
    override fun foo() {

    }
}

class SS {

}