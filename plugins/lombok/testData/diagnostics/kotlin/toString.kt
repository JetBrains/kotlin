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

@ToString
class WithBothIncludeAndExclude(
    <!TO_STRING_EXCLUDE_AND_INCLUDE!>@ToString.Include<!> @ToString.Exclude val conflicting: String,
    val normal: String,
)

@ToString
class WithOnlyInclude(@ToString.Include val included: String)

@ToString
class WithOnlyExclude(@ToString.Exclude val excluded: String, val normal: String)

// No warning: doNotUseGetters not specified
@ToString
class Normal(val x: Int)

// TO_STRING_DO_NOT_USE_GETTERS_IRRELEVANT warning: doNotUseGetters = true is Java-specific and has no effect in Kotlin
@ToString(doNotUseGetters = <!TO_STRING_DO_NOT_USE_GETTERS_IRRELEVANT!>true<!>)
class WithDoNotUseGettersTrue(val x: Int)

// TO_STRING_DO_NOT_USE_GETTERS_IRRELEVANT warning: doNotUseGetters = false is Java-specific.
// Despite the absence of behavioral difference, report a warning because the parameter is redundant and it's discrouraged to use in Kotlin.
@ToString(doNotUseGetters = <!TO_STRING_DO_NOT_USE_GETTERS_IRRELEVANT!>false<!>)
class WithDoNotUseGettersFalse(val x: Int)
