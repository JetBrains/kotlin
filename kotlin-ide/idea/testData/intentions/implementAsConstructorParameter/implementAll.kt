// WITH_RUNTIME
// DISABLE-ERRORS
interface T<X> {
    val <caret>foo: X
}

class U : T<String> {

}

class V : T<Int> {

}

class Z : T<Int> by V() {

}

class W : T<Boolean> {
    override val foo: Boolean
        get() = throw UnsupportedOperationException()
}