class U<A>

interface T<A, B> {
    fun <C> foofoofoo(a: List<C>, b: A?, c: U<B>): U<C>?
}

abstract class T1<X, Y> : T<U<X>, U<Y>> {
    override fun <C> foofoofoo(a: List<C>, b: U<X>?, c: U<U<Y>>): U<C>? {
        throw UnsupportedOperationException()
    }
}

abstract class T2<X> : T1<X, String>() {
    override fun <C> foofoofoo(a: List<C>, b: U<X>?, c: U<U<String>>): U<C>? {
        throw UnsupportedOperationException()
    }
}

class T3 : T2<Any>() {
    override fun <D> foofoofoo(a: List<D>, b: U<Any>?, c: U<U<String>>): U<D>? {
        throw UnsupportedOperationException()
    }
}