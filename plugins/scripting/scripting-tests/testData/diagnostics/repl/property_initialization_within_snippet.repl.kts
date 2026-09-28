// WITH_STDLIB
// DIAGNOSTICS: -WRONG_INVOCATION_KIND

// SNIPPET

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlin.properties.ReadOnlyProperty

@OptIn(ExperimentalContracts::class)
inline fun <T> inPlaceRun(block: () -> T): T {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return block()
}

fun <T> notInPlaceRun(block: () -> T): T = null!!

fun <T> simpleDelegate(value: T): ReadOnlyProperty<Any?, T> = null!!

@OptIn(ExperimentalContracts::class)
fun <T> inPlaceDelegate(block: () -> T): ReadOnlyProperty<Any?, T> {
    contract { callsInPlace(block, InvocationKind.EXACTLY_ONCE) }
    return null!!
}

fun <T> notInPlaceDelegate(block: () -> T): ReadOnlyProperty<Any?, T> = null!!

val a: String = <!UNINITIALIZED_VARIABLE!>a<!>
val b: String = inPlaceRun { <!UNINITIALIZED_VARIABLE!>b<!> }
val c: String = notInPlaceRun { c }

val d: String by simpleDelegate(<!UNINITIALIZED_VARIABLE!>d<!>)
val e: String by inPlaceDelegate { <!UNINITIALIZED_VARIABLE!>e<!> }
val f: String by notInPlaceDelegate { f }

val l: Comparator<String> = object : Comparator<String> {
    val delegate: Comparator<String> get() = n
    override fun compare(o1: String, o2: String): Int = delegate.compare(o1, o2)
}
val m: Comparator<String> = object : Comparator<String> by <!UNINITIALIZED_VARIABLE!>n<!> {}
val n: Comparator<String> = Comparator { _, _ -> 0 }

val t: String = <!UNINITIALIZED_VARIABLE!>z<!>
val u: String = inPlaceRun { <!UNINITIALIZED_VARIABLE!>z<!> }
val v: String = notInPlaceRun { z }
val w: String by simpleDelegate(<!UNINITIALIZED_VARIABLE!>z<!>)
val x: String by inPlaceDelegate { <!UNINITIALIZED_VARIABLE!>z<!> }
val y: String by notInPlaceDelegate { z }
val z: String = "VALUE"
