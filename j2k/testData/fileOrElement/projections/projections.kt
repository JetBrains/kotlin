import java.util.ArrayList

internal class C<T> {
    internal fun foo1(src: Collection<T>) {
        val t = src.iterator().next()
    }

    internal fun foo2(src: ArrayList<out T>) {
        val t = src.iterator().next()
    }

    internal fun foo3(dst: MutableCollection<in T>, t: T) {
        dst.add(t)
    }

    internal fun foo4(comparable: Comparable<T>, t: T): Int {
        return comparable.compareTo(t)
    }

    internal fun foo5(w: Collection<*>) {
    }
}
