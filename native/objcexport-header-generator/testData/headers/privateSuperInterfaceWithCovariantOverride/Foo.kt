private interface PrivateA {
    fun foo(): Any
}

class PublicB : PrivateA {
    override fun foo() : String = error("stub")
}