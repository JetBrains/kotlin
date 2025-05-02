
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

// The CONFLICTING_JVM_DECLARATIONS should be reported here, but it is not happening due to KT-77362
// NOTE: after fixing it, some changes in AbstractReplWithTestExtensionsDiagnosticsTest needed to report it too
val B<Int>.x by CustomDelegate()

var B<String>.x by CustomDelegate()

