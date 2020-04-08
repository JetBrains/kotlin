open class T {
    open var <caret>foo: String = ""
}

open class X: T() {
    override var foo: String
        get() = ""
        set(value: String) {}
}

interface Y: T {
    override var foo: String
        get() = ""
        set(value: String) {}
}

open class Z: Y {
    override var foo: String
        get() = ""
        set(value: String) {}
}

class SS {

}