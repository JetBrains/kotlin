package test

open class B {
    val fromB1: String
        get() = ""
    var fromB2: String?
        get() = ""
        set(value) {}
    var fromB3: String?
        get() = ""
        set(value) {}
    var fromB4: String?
        get() = ""
        set(value) {}

    open fun setFromB5(value: String?) {}
}