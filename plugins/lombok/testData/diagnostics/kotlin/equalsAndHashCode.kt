// RENDER_DIAGNOSTICS_FULL_TEXT

<!ANNOTATION_HAS_NO_EFFECT!>@file:EqualsAndHashCode<!>

import lombok.EqualsAndHashCode

<!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode<!>
interface Interface

<!WRONG_ANNOTATION_TARGET!>@EqualsAndHashCode<!>
fun func() {}

<!WRONG_ANNOTATION_TARGET!>@EqualsAndHashCode<!>
typealias TA = String

val onAnonymous = <!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode<!> object {}

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

// No warning: doNotUseGetters not specified
@EqualsAndHashCode
class Normal(val x: Int)

// doNotUseGetters is Java-specific and has no effect in Kotlin
@EqualsAndHashCode(doNotUseGetters = <!DO_NOT_USE_GETTERS_IRRELEVANT!>true<!>)
class WithDoNotUseGettersTrue(val x: Int)

@EqualsAndHashCode(doNotUseGetters = <!DO_NOT_USE_GETTERS_IRRELEVANT!>false<!>)
class WithDoNotUseGettersFalse(val x: Int)
