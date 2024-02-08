class A<T> {
    fun <T> a(t: T) {}

    fun <U> f(m: Map.Entry<T, U>) {}

    inner class B<U, V : U> {
        fun <T, U> b(t: T, u: U & Any, v: V) where T : Comparable<T>, T : Cloneable {}

        fun bb(t: T) {}

        inner class C<T, U> {
            fun <T, U> c(t: T?, u: U?) {}

            fun cc(t: T?, u: U?) {}

            fun z(c: A<Int>.B<Any, Byte>.C<Unit, Long>) {}
        }
    }
}
