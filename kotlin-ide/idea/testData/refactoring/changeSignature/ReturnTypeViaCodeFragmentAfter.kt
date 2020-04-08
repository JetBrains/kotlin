class A<T, U>

interface I {
    fun <T, U> bar(): A<T, U>
}

class C : I {
    override fun <V, W> bar() = A<V, W>()
}