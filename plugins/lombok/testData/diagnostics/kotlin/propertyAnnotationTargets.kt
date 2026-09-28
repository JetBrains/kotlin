// WITH_STDLIB
// ISSUE: KT-89040

import lombok.Builder
import lombok.EqualsAndHashCode
import lombok.Singular
import lombok.ToString

<!ANNOTATION_HAS_NO_EFFECT!>@ToString.Include<!>
val topLevel: Int = 43

<!ANNOTATION_HAS_NO_EFFECT!>@EqualsAndHashCode.Exclude<!>
val topLevelEqualsExclude: Int = 1

<!ANNOTATION_HAS_NO_EFFECT!>@Singular<!>
val topLevelSingular: List<String> = emptyList()

<!ANNOTATION_HAS_NO_EFFECT!>@Builder.Default<!>
val topLevelBuilderDefault: Int = 1

// top-level, explicit field use-site
<!ANNOTATION_HAS_NO_EFFECT!>@field:ToString.Include<!>
val topLevelExplicitField: Int = 1

// top-level without a backing field
<!WRONG_ANNOTATION_TARGET!>@ToString.Include<!>
val getterOnly: Int get() = 1

<!WRONG_ANNOTATION_TARGET!>@ToString.Include<!>
val delegated: Int by lazy { 1 }

enum class OnEnumEntry {
    <!ANNOTATION_HAS_NO_EFFECT!>@ToString.Include<!>
    ENTRY
}

fun locals() {
    <!WRONG_ANNOTATION_TARGET!>@ToString.Include<!>
    val localInclude: Int = 1
    localInclude.toString()
}
