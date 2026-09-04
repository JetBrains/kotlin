// LL_FIR_DIVERGENCE
// KT-85026: no multi-snippet support yet
// LL_FIR_DIVERGENCE
// DIAGNOSTICS: -CONFLICTING_JVM_DECLARATIONS

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

// The CONFLICTING_JVM_DECLARATIONS is reported there by the backend, but it is invoked only in `AbstractReplViaApiDiagnosticsTest`.
// `AbstractReplWithTestExtensionsDiagnosticsTest` invokes only frontend, so the error is not reported.
// To avoid the discrepancy between tests, this specific diagnostic is disabled on the test level.
val <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>B<!><Int>.x by <!UNRESOLVED_REFERENCE!>CustomDelegate<!>()

var <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>B<!><String>.x by <!UNRESOLVED_REFERENCE!>CustomDelegate<!>()

