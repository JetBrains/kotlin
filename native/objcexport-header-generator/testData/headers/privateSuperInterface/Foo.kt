private interface PrivateA {
    fun foo()
}

class PublicB : PrivateA {
    override fun foo() = Unit
}