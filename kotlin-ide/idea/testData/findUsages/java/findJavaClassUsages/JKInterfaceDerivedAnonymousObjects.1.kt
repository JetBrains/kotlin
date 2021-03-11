fun foo() {
    open class X: A

    val a = object: A

    fun bar() {
        val b = object: X()
    }
}



