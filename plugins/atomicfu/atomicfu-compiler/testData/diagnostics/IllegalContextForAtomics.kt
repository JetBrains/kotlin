// RENDER_DIAGNOSTICS_FULL_TEXT

import kotlinx.atomicfu.*

//// Local atomic variables are prohibited
fun localDeclarations() {
    val i = atomic(0)
    val l = atomic(0L)
    val r = atomic("ref")
    val b = atomic(true)
    val ia = AtomicIntArray(0)
    val la = AtomicLongArray(0)
    val ra = atomicArrayOfNulls<String?>(0)
    val ba = AtomicBooleanArray(0)

    val ni: AtomicInt? = null
    val nl: AtomicLong? = null
    val nb: AtomicBoolean? = null
    val nr: AtomicRef<Any?>? = null

    val nia: AtomicIntArray? = null
    val nla: AtomicLongArray? = null
    val nba: AtomicBooleanArray? = null
    val naa: AtomicArray<Any?>? = null
}

// Creating a regular array of AFU's atomics is forbidden
fun arrayOfAtomics() {
    val a1 = arrayOf(<!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(0)<!>, <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(1)<!>)
    Array(42) { <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic("$it")<!> }
    val a3 = Array(1) { <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicIntArray(10)<!> }
}

// One can't call atomic factory / ctor without assigning a result
fun callWithoutAssignment() {
    <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(42)<!>
    <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomicArrayOfNulls<String?>(1)<!>
    <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>AtomicBooleanArray(2)<!>
}

// Calling atomic factory to assign default value in constructor is unsupported
abstract class AbstractAtomicPropertyInCtor(private val a: AtomicInt = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(0)<!>)
class AtomicInCtor(private val a: AtomicLong = <!ATOMIC_FACTORIES_ARE_FOR_INITIALIZATION_ONLY!>atomic(0L)<!>)

abstract class AbstractAtomicProperty {
    // Non private property, has no backing field
    <!PUBLIC_ATOMICS_ARE_FORBIDDEN!>protected<!> abstract <!ATOMIC_PROPERTIES_MUST_HAVE_BACKING_FIELD!>val<!> a: AtomicInt
}

