<!WRONG_ANNOTATION_TARGET!>@file:ToString<!>

import lombok.ToString

<!WRONG_ANNOTATION_TARGET!>@ToString<!>
interface Interface

<!WRONG_ANNOTATION_TARGET!>@ToString<!>
fun func() {}

<!WRONG_ANNOTATION_TARGET!>@ToString<!>
typealias TA = String

val toStringOnAnonymousObject = <!WRONG_ANNOTATION_TARGET!>@ToString<!> object {}

<!TO_STRING_FUNCTION_ALREADY_EXISTS!>@ToString<!>
class WithExistingToString(val x: Int) {
    override fun toString(): String = "custom"
}

@ToString
class WithExistingNonConflictingToString(val x: Int) {
    fun toString(p: Boolean): String = if (p) super.toString() else "custom"
}

@ToString
class WithNonConflictingExtensionFunction {
    fun WithNonConflictingExtensionFunction.<!EXTENSION_SHADOWED_BY_MEMBER!>toString<!>(): String = "Ext"
}

@ToString
class WithNonConflictingContextualFunction {
    context(p: WithNonConflictingContextualFunction)
    fun toString(): String = "Contex"
}
