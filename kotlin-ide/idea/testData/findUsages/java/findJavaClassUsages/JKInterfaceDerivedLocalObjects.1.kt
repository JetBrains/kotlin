fun foo() {
    open class T: A

    object O1: A

    fun bar() {
        object O2: T()
    }
}



