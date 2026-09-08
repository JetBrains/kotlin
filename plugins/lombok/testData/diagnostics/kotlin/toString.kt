<!ANNOTATION_HAS_NO_EFFECT!>@file:ToString<!>

import lombok.ToString

<!ANNOTATION_HAS_NO_EFFECT!>@ToString<!>
interface Interface

<!ANNOTATION_HAS_NO_EFFECT!>@ToString<!>
annotation class AnnotationClass

<!WRONG_ANNOTATION_TARGET!>@ToString<!>
fun func() {}

<!WRONG_ANNOTATION_TARGET!>@ToString<!>
typealias TA = String

val toStringOnAnonymousObject = <!ANNOTATION_HAS_NO_EFFECT!>@ToString<!> object {}

val toStringOnLiteral = <!ANNOTATION_HAS_NO_EFFECT!>@ToString<!> 1
val toStringOnCall = <!ANNOTATION_HAS_NO_EFFECT!>@ToString<!> func()

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
    <!EXCLUDE_AND_INCLUDE_MUTUALLY_EXCLUSIVE!>@ToString.Include<!> @ToString.Exclude val conflicting: String,
    val normal: String,
)

@ToString
class WithOnlyInclude(@ToString.Include val included: String)

@ToString
class WithOnlyExclude(@ToString.Exclude val excluded: String, val normal: String)

// A `$`-prefixed property is left out of what Lombok generates unless it is explicitly included, so an
// `@Exclude` on one says nothing the name does not already say, KT-88636.
@ToString
class WithDollarPrefixedProperties(
    val regular: String,
    val `$excludedByDefault`: String,
    @ToString.Include val `$explicitlyIncluded`: String,
    <!EXCLUDE_IS_REDUNDANT_FOR_DOLLAR_PREFIXED_PROPERTY!>@ToString.Exclude<!> val `$redundantlyExcluded`: String,
)

// Both diagnostics are reported, exactly as Lombok reports both.
@ToString
class WithDollarPrefixedPropertyIncludedAndExcluded(
    <!EXCLUDE_AND_INCLUDE_MUTUALLY_EXCLUSIVE!>@ToString.Include<!>
    <!EXCLUDE_IS_REDUNDANT_FOR_DOLLAR_PREFIXED_PROPERTY!>@ToString.Exclude<!>
    val `$both`: String,
)

// No warning: the name does not start with `$`, so the `@Exclude` is doing the work.
@ToString
class WithRegularExcludedProperty(val regular: String, @ToString.Exclude val excluded: String)

// `onlyExplicitlyIncluded` leaves nothing for an `@Exclude` to take out - a property is in only if it says
// `@Include` - so the annotation says nothing the class-level argument does not already say, KT-88655.
@ToString(onlyExplicitlyIncluded = true)
class OnlyExplicitlyIncludedExample(
    <!EXCLUDE_IS_REDUNDANT_FOR_ONLY_EXPLICITLY_INCLUDED!>@ToString.Exclude<!> val id: Long,
    val name: String,
)

// Only one redundancy is reported per property, `onlyExplicitlyIncluded` first: Lombok chains the two with
// `else if`, and once the whole class is opt-in there is nothing left for `$` to explain.
@ToString(onlyExplicitlyIncluded = true)
class OnlyExplicitlyIncludedDollarPrefixed(
    <!EXCLUDE_IS_REDUNDANT_FOR_ONLY_EXPLICITLY_INCLUDED!>@ToString.Exclude<!> val `$dollarPrefixed`: String,
)

// The clash is reported on top of it, the two being separate checks in Lombok as well.
@ToString(onlyExplicitlyIncluded = true)
class OnlyExplicitlyIncludedIncludedAndExcluded(
    <!EXCLUDE_AND_INCLUDE_MUTUALLY_EXCLUSIVE!>@ToString.Include<!>
    <!EXCLUDE_IS_REDUNDANT_FOR_ONLY_EXPLICITLY_INCLUDED!>@ToString.Exclude<!>
    val both: String,
)

// No warning: `onlyExplicitlyIncluded = false` puts every property back in, so the `@Exclude` does the work.
@ToString(onlyExplicitlyIncluded = false)
class OnlyExplicitlyIncludedFalse(@ToString.Exclude val excluded: String, val included: String)

// No CALL_SUPER_NOT_CALLED warning: `lombok.toString.callSuper` defaults to `skip`, unlike
// `lombok.equalsAndHashCode.callSuper`, which defaults to `warn`, KT-88653.
open class Base(val baseProp: Int)

@ToString
class DerivedImplicit(val ownProp: String) : Base(10)

// No warning: doNotUseGetters not specified
@ToString
class Normal(val x: Int)

// DO_NOT_USE_GETTERS_IRRELEVANT warning: doNotUseGetters = true is Java-specific and has no effect in Kotlin
@ToString(doNotUseGetters = <!DO_NOT_USE_GETTERS_IRRELEVANT!>true<!>)
class WithDoNotUseGettersTrue(val x: Int)

// DO_NOT_USE_GETTERS_IRRELEVANT warning: doNotUseGetters = false is Java-specific.
// Despite the absence of behavioral difference, report a warning because the parameter is redundant and it's discrouraged to use in Kotlin.
@ToString(doNotUseGetters = <!DO_NOT_USE_GETTERS_IRRELEVANT!>false<!>)
class WithDoNotUseGettersFalse(val x: Int)

@ToString(
    exclude = <!ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED!>[]<!>,
    of = <!ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED!>[]<!>,
)
class WithUnsupportedArguments
