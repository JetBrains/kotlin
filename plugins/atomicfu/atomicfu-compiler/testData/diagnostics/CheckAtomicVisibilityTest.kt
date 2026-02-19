import kotlinx.atomicfu.*
import kotlin.test.*

class A {
    <!PUBLIC_ATOMICS_ARE_FORBIDDEN!>public<!> val a = atomic(true)
    @PublishedApi <!PUBLISHED_API_ATOMICS_ARE_FORBIDDEN!>internal<!> val b = atomic(false)
    <!PUBLIC_ATOMICS_ARE_FORBIDDEN!>public<!> <!ATOMIC_PROPERTIES_SHOULD_BE_VAL!>var<!> c = atomic(false)
    internal val d = atomic(0)

    public inline fun update(newValue: Boolean, lambda: (Boolean) -> Unit) {
        val oldValue = b.getAndSet(newValue)
        lambda(oldValue)
    }
}

@PublishedApi
internal class A1 {
    val <!PUBLISHED_API_ATOMICS_ARE_FORBIDDEN!>a<!> = atomic(0)
    @PublishedApi <!PUBLISHED_API_ATOMICS_ARE_FORBIDDEN!>internal<!> val b = atomic(false)
    internal val c = atomic(0)
}

internal class A2 {
    val a = atomic(0)
    @PublishedApi internal val b = atomic(0)
    internal val c = atomic(0)
}

private class A3 {
    val a = atomic(0)
    @PublishedApi internal val b = atomic(0)
    internal val c = atomic(0)
}

abstract class Base {
    val <!PUBLIC_ATOMICS_ARE_FORBIDDEN!>a<!> = atomic(0)
    @PublishedApi <!PUBLISHED_API_ATOMICS_ARE_FORBIDDEN!>internal<!> val b = atomic(0)
    internal val c = atomic(0)
    <!PUBLIC_ATOMICS_ARE_FORBIDDEN!>protected<!> val d = atomic(0)

    protected class Nested {
        val <!PUBLIC_ATOMICS_ARE_FORBIDDEN!>nestedA<!> = atomic(0)
        @PublishedApi <!PUBLISHED_API_ATOMICS_ARE_FORBIDDEN!>internal<!> val nestedB = atomic(0)
        internal val nestedC = atomic(0)
        <!PUBLIC_ATOMICS_ARE_FORBIDDEN!>protected<!> val nestedD = atomic(0)
    }
}

fun box(): String {
    val aClass = A()
    aClass.update(true) { i -> assertFalse(i) }
    return "OK"
}
