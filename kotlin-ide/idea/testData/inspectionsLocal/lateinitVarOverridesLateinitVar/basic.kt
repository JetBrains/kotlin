// FIX: none
open class A1 {
    open lateinit var a: String
}

class A2 : A1() {
    override lateinit var <caret>a: String
}