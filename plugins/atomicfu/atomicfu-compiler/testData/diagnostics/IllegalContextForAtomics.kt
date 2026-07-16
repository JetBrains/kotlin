// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*

fun localDeclarations() {
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> i = atomic(0)
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> l = atomic(0L)
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> r = atomic("ref")
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> b = atomic(true)
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> ia = AtomicIntArray(0)
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> la = AtomicLongArray(0)
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> ra = atomicArrayOfNulls<String?>(0)
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> ba = AtomicBooleanArray(0)

    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> ni: AtomicInt? = null
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> nl: AtomicLong? = null
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> nb: AtomicBoolean? = null
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> nr: AtomicRef<Any?>? = null

    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> nia: AtomicIntArray? = null
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> nla: AtomicLongArray? = null
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> nba: AtomicBooleanArray? = null
    <!ATOMIC_LOCALS_ARE_FORBIDDEN!>val<!> naa: AtomicArray<Any?>? = null
}

fun arrayOfAtomics() {
    val a1 = arrayOf(atomic(0), atomic(1))
    Array(42) { atomic("$it") }
    val a3 = Array(1) { AtomicIntArray(10) }
}

abstract class AbstractAtomicPropertyInCtor(private val <!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>a<!>: AtomicInt = atomic(0))

abstract class AbstractAtomicProperty {
    <!PUBLIC_ATOMICS_ARE_FORBIDDEN!>protected<!> abstract <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> a: AtomicInt
}

class AtomicInCtor(private val <!ATOMIC_VALUE_PARAMETERS_ARE_FORBIDDEN!>a<!>: AtomicLong = atomic(0L))
