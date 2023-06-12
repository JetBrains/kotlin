// DONT_TARGET_EXACT_BACKEND: JS

abstract class Foo<out E>: Collection<E> {
    abstract fun foo(element: @UnsafeVariance E): Boolean
    abstract override fun contains(element: @UnsafeVariance E): Boolean
}

class Bar<E : C> : Foo<E>() {
    override fun foo(element: E): Boolean {
        if (element !is C?) return false
        return true
    }

    override fun contains(element: E): Boolean {
        if (element !is C?) return false
        return true
    }

    override val size: Int get() = -1
    override fun isEmpty() = true
    override fun containsAll(elements: Collection<E>) = false
    override fun iterator(): Iterator<E> = TODO("Not yet implemented")
}

open class C

open class D : C()

fun case1(): Int {
    val a = (object{})
    val foo: Foo<Any?> = Bar<D>()
    try {
        if (foo.foo(a as Any?))
            return 1
        return 2
    } catch (e: ClassCastException) {
        return 0
    }
}

fun case2(): Int {
    val a = (object{})
    val foo: Collection<Any?> = Bar<D>()
    // compiler knows about this "special" method and it adds an implicit type check which prevents class cast exception
    if (foo.contains(a as Any?))
        return 1
    return 0
}

fun box(): String {
    var r = case1()
    if (r != 0) return "Fail case 1, got $r"

    r = case2()
    if (r != 0) return "Fail case 2, got $r"

    return "OK"
}
