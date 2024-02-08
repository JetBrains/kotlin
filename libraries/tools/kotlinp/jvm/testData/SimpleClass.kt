// IGNORE K2

class SimpleClass<in A>(val p: Int = 42) {
    constructor(s: Array<String?>?) : this(s?.size ?: 0)

    var x: Long = p.toLong()
        external get
        @JvmName("SET_X") set

    internal fun <U : A, V, A> A.f(vararg z: Map<V, U?>): Set<*> where V : A {
        error("")
    }

    protected suspend inline fun <reified T> g(crossinline a: () -> A, noinline b: suspend () -> T) {}
}
