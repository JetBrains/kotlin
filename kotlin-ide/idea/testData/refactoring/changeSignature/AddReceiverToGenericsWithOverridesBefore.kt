class U<A>

interface T<A> {
    fun <caret>foofoofoo<B>(a: A, b: B): Int
}

abstract class T1<X> : T<U<X>> {
    override fun <B> foofoofoo(a: U<X>, b: B): Int {
        throw UnsupportedOperationException()
    }
}

abstract class T2 : T1<String>() {
    override fun <C> foofoofoo(a: U<String>, b: C): Int {
        throw UnsupportedOperationException()
    }
}