private interface PrivateInterface<T> {
    fun foo(): T
}

class PublicClass : PrivateInterface<Int> {
    override fun foo(): Int = error("stub")
}