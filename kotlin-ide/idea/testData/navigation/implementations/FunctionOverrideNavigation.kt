package testing

open class Base {
    open fun <caret>test() {
    }
}

open class SubBase: Base() {
    override fun test() {

    }
}

class SubSubBase: SubBase() {
    override fun test() {
    }
}


// REF: (in testing.SubBase).test()
// REF: (in testing.SubSubBase).test()
