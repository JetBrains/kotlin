// RUN_PIPELINE_TILL: FRONTEND
import org.jetbrains.kotlin.plugin.sandbox.CompanionWithFoo

@CompanionWithFoo
class <!COMPILER_PLUGIN_REDECLARATION!>SomeClass<!> {
    object <!REDECLARATION!>Companion<!>
}

fun takeInt(x: Int) {}

fun test() {
    takeInt(SomeClass.<!UNRESOLVED_REFERENCE!>foo<!>())
    takeInt(SomeClass.<!OVERLOAD_RESOLUTION_AMBIGUITY!>Companion<!>.<!UNRESOLVED_REFERENCE!>foo<!>())
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration */
