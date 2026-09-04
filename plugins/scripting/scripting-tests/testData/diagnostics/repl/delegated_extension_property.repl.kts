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

val A.<!REDECLARATION!>x<!> by CustomDelegate()

var A.<!REDECLARATION!>x<!> by CustomDelegate()

// SNIPPET

// The CONFLICTING_JVM_DECLARATIONS is reported there by the backend, but it is invoked only in `AbstractReplViaApiDiagnosticsTest`.
// `AbstractReplWithTestExtensionsDiagnosticsTest` invokes only frontend, so the error is not reported.
// To avoid the discrepancy between tests, this specific diagnostic is disabled on the test level.
val B<Int>.x by CustomDelegate()

var B<String>.x by CustomDelegate()

