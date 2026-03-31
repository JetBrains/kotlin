// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE

// SNIPPET

import kotlin.reflect.KProperty

class A

class B<T>

class CustomDelegate {
    private var value: String = "OK"

    operator fun getValue(thisRef: Any?, property: KProperty<*>): String {
        return value
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, newValue: String) {
        value = newValue
    }
}

// SNIPPET

val <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>A<!>.x by <!UNRESOLVED_REFERENCE!>CustomDelegate<!>()

var <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>A<!>.x by <!UNRESOLVED_REFERENCE!>CustomDelegate<!>()

// SNIPPET

// The CONFLICTING_JVM_DECLARATIONS should be reported here, but it is not happening due to KT-77362
// NOTE: after fixing it, some changes in AbstractReplWithTestExtensionsDiagnosticsTest needed to report it too
val <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>B<!><Int>.x by <!UNRESOLVED_REFERENCE!>CustomDelegate<!>()

var <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>B<!><String>.x by <!UNRESOLVED_REFERENCE!>CustomDelegate<!>()

