// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: TYPE_MISMATCH
// !MESSAGE_TYPE: HTML

package a.b.c

class G

typealias A = G

class D<T, A> {
    fun g(t: Map<T, a.b.c.A>) {
    }

    fun <T> g(t1: Map<T, A>, t2: Int) {
        g(t1)
    }
}