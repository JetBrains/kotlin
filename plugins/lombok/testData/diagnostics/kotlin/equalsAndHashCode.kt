// RENDER_DIAGNOSTICS_FULL_TEXT

<!ANNOTATION_HAS_NO_EFFECT!>@file:EqualsAndHashCode<!>

import lombok.EqualsAndHashCode

<!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode<!>
interface Interface

<!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode<!>
annotation class AnnotationClass

<!WRONG_ANNOTATION_TARGET!>@EqualsAndHashCode<!>
fun func() {}

<!WRONG_ANNOTATION_TARGET!>@EqualsAndHashCode<!>
typealias TA = String

val onAnonymous = <!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode<!> object {}

// An enum inherits final `equals`/`hashCode` from `java.lang.Enum`, so a generated one wouldn't load, KT-88507.
<!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode<!>
enum class Color(val hex: String) {
    RED("#FF0000")
}

// An object is a single instance, and comparing it to itself by identity is what it already does, KT-88507.
<!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode<!>
object Object {
    val version = "2.0"
}

class WithCompanion {
    <!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode<!>
    companion object {
        val version = "2.0"
    }
}

// Both equals and hashCode user-defined → warning, no generation.
<!EQUALS_OR_HASH_CODE_FUNCTIONS_ALREADY_EXIST!>@EqualsAndHashCode<!>
class WithBothExisting(val x: Int) {
    override fun equals(other: Any?): Boolean = (other as? WithBothExisting)?.x == x
    override fun hashCode(): Int = x
}

// Only equals user-defined → warning, no generation.
<!EQUALS_OR_HASH_CODE_FUNCTIONS_ALREADY_EXIST!>@EqualsAndHashCode<!>
class WithOnlyEquals(val x: Int) {
    override fun equals(other: Any?): Boolean = (other as? WithOnlyEquals)?.x == x
}

// Only hashCode user-defined → warning, no generation.
<!EQUALS_OR_HASH_CODE_FUNCTIONS_ALREADY_EXIST!>@EqualsAndHashCode<!>
class WithOnlyHashCode(val x: Int) {
    override fun hashCode(): Int = x
}

// Both equals and hashCode user-defined on a data class → warning, no generation.
<!EQUALS_OR_HASH_CODE_FUNCTIONS_ALREADY_EXIST!>@EqualsAndHashCode<!>
data class WithDataClassBothExisting(val x: Int) {
    override fun equals(other: Any?): Boolean = (other as? WithBothExisting)?.x == x
    override fun hashCode(): Int = x
}

// No EQUALS_OR_HASH_CODE_FUNCTIONS_ALREADY_EXIST warning, generate equals and hashCode even on the data class.
@EqualsAndHashCode
data class WithDataClassGeneration(val x: Int) {
}

@EqualsAndHashCode
class WithBothIncludeAndExclude(
    <!EXCLUDE_AND_INCLUDE_MUTUALLY_EXCLUSIVE!>@EqualsAndHashCode.Include<!> @EqualsAndHashCode.Exclude val conflicting: String,
    val normal: String,
)

@EqualsAndHashCode
class WithOnlyInclude(@EqualsAndHashCode.Include val included: String)

@EqualsAndHashCode
class WithOnlyExclude(@EqualsAndHashCode.Exclude val excluded: String, val normal: String)

// A `$`-prefixed property is left out of what Lombok generates unless it is explicitly included, so an
// `@Exclude` on one says nothing the name does not already say, KT-88636.
@EqualsAndHashCode
class WithDollarPrefixedProperties(
    val regular: String,
    val `$excludedByDefault`: String,
    @EqualsAndHashCode.Include val `$explicitlyIncluded`: String,
    <!EXCLUDE_IS_REDUNDANT_FOR_DOLLAR_PREFIXED_PROPERTY!>@EqualsAndHashCode.Exclude<!> val `$redundantlyExcluded`: String,
)

// Both diagnostics are reported, exactly as Lombok reports both.
@EqualsAndHashCode
class WithDollarPrefixedPropertyIncludedAndExcluded(
    <!EXCLUDE_AND_INCLUDE_MUTUALLY_EXCLUSIVE!>@EqualsAndHashCode.Include<!>
    <!EXCLUDE_IS_REDUNDANT_FOR_DOLLAR_PREFIXED_PROPERTY!>@EqualsAndHashCode.Exclude<!>
    val `$both`: String,
)

// No warning: the name does not start with `$`, so the `@Exclude` is doing the work.
@EqualsAndHashCode
class WithRegularExcludedProperty(val regular: String, @EqualsAndHashCode.Exclude val excluded: String)

// `onlyExplicitlyIncluded` leaves nothing for an `@Exclude` to take out - a property is in only if it says
// `@Include` - so the annotation says nothing the class-level argument does not already say, KT-88655.
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
class OnlyExplicitlyIncludedExample(
    <!EXCLUDE_IS_REDUNDANT_FOR_ONLY_EXPLICITLY_INCLUDED!>@EqualsAndHashCode.Exclude<!> val id: Long,
    val name: String,
)

// Only one redundancy is reported per property, `onlyExplicitlyIncluded` first: Lombok chains the two with
// `else if`, and once the whole class is opt-in there is nothing left for `$` to explain.
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
class OnlyExplicitlyIncludedDollarPrefixed(
    <!EXCLUDE_IS_REDUNDANT_FOR_ONLY_EXPLICITLY_INCLUDED!>@EqualsAndHashCode.Exclude<!> val `$dollarPrefixed`: String,
)

// The clash is reported on top of it, the two being separate checks in Lombok as well.
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
class OnlyExplicitlyIncludedIncludedAndExcluded(
    <!EXCLUDE_AND_INCLUDE_MUTUALLY_EXCLUSIVE!>@EqualsAndHashCode.Include<!>
    <!EXCLUDE_IS_REDUNDANT_FOR_ONLY_EXPLICITLY_INCLUDED!>@EqualsAndHashCode.Exclude<!>
    val both: String,
)

// No warning: `onlyExplicitlyIncluded = false` puts every property back in, so the `@Exclude` does the work.
@EqualsAndHashCode(onlyExplicitlyIncluded = false)
class OnlyExplicitlyIncludedFalse(@EqualsAndHashCode.Exclude val excluded: String, val included: String)

// No warning: doNotUseGetters not specified
@EqualsAndHashCode
class Normal(val x: Int)

// doNotUseGetters is Java-specific and has no effect in Kotlin
@EqualsAndHashCode(doNotUseGetters = <!DO_NOT_USE_GETTERS_IRRELEVANT!>true<!>)
class WithDoNotUseGettersTrue(val x: Int)

@EqualsAndHashCode(doNotUseGetters = <!DO_NOT_USE_GETTERS_IRRELEVANT!>false<!>)
class WithDoNotUseGettersFalse(val x: Int)

@EqualsAndHashCode(
    exclude = <!ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED!>[]<!>,
    of = <!ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED!>[]<!>,
    doNotUseGetters = <!DO_NOT_USE_GETTERS_IRRELEVANT!>true<!>,
    cacheStrategy = <!ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED!>EqualsAndHashCode.CacheStrategy.LAZY<!>,
    onParam = <!ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED!>[]<!>,
)
class UnsupportedArguments(
    @EqualsAndHashCode.Include(
        replaces = <!ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED!>"something"<!>,
        rank = <!ANNOTATION_ARGUMENT_IS_NOT_SUPPORTED!>42<!>
    )
    val x: Int
)
