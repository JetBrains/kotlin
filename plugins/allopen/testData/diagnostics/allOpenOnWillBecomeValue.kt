// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// OPT_IN: kotlin.ExperimentalValueClassesApi

annotation class AllOpen

@AllOpen
@WillBecomeValue
class <!WILL_BECOME_VALUE_CLASS_OPEN!>ImplicitlyOpen<!>(val x: String) {
    override fun hashCode() = 0
    override fun toString() = ""
    override fun equals(other: Any?) = other is ImplicitlyOpen
}

@AllOpen
@WillBecomeValue
final class ExplicitlyFinal(val x: String) {
    override fun hashCode() = 0
    override fun toString() = ""
    override fun equals(other: Any?) = other is ExplicitlyFinal
}

/* GENERATED_FIR_TAGS: annotationDeclaration, classDeclaration, functionDeclaration, integerLiteral, isExpression,
nullableType, operator, override, primaryConstructor, propertyDeclaration, stringLiteral */
