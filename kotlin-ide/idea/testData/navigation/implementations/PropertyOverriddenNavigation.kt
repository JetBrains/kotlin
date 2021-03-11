package testing

open class Base {
    open var <caret>test = 12
}

open class SubBase: Base() {
    override var test = 12
}

class SubSubBase: SubBase() {
    override var test = 12
}


// REF: (in testing.SubBase).test
// REF: (in testing.SubSubBase).test