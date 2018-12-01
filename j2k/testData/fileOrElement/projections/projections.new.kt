internal class C<T> {
    fun foo1(src: Collection<T?>) {
        val t = src.iterator().next()
    }

    fun foo2(src: ArrayList<out T?>) {
        val t = src.iterator().next()
    }

    fun foo3(dst: MutableCollection<in T?>, t: T?) {
        dst.add(t)
    }

    fun foo4(comparable: Comparable<T?>, t: T?): Int {
        return comparable.compareTo(t)
    }

    fun foo5(w: Collection<*>?) {}
}