// ISSUE: KT-89024
// WITH_STDLIB

import lombok.Builder

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder(toBuilder = true, builderMethodName = "")<!>
class Suppressed(val x: Int)

<!ANNOTATION_IS_NOT_SUPPORTED!>@Builder(builderMethodName = "createBuilder")<!>
class Renamed(val x: Int)

fun testSuppressed(instance: Suppressed) {
    // Lombok generates no `builder()` for an empty `builderMethodName`, and with it gone the class is
    // left with nothing to put into a companion object, so it gets none either.
    Suppressed.<!UNRESOLVED_REFERENCE!>builder<!>()
    Suppressed.<!UNRESOLVED_REFERENCE!>Companion<!>

    // Everything else the annotation generates is untouched.
    val builder: Suppressed.<!UNRESOLVED_REFERENCE!>SuppressedBuilder<!> = instance.<!UNRESOLVED_REFERENCE!>toBuilder<!>()
    builder.<!UNRESOLVED_REFERENCE!>x<!>(1).build()
}

fun testRenamed() {
    val builder: Renamed.<!UNRESOLVED_REFERENCE!>RenamedBuilder<!> = Renamed.<!UNRESOLVED_REFERENCE!>createBuilder<!>()
    builder.<!UNRESOLVED_REFERENCE!>x<!>(1).build()
}
