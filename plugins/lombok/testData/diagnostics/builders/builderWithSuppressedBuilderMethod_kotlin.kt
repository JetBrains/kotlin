// ISSUE: KT-89024
// WITH_STDLIB

import lombok.Builder

@Builder(toBuilder = true, builderMethodName = "")
class Suppressed(val x: Int)

@Builder(builderMethodName = "createBuilder")
class Renamed(val x: Int)

fun testSuppressed(instance: Suppressed) {
    // Lombok generates no `builder()` for an empty `builderMethodName`, and with it gone the class is
    // left with nothing to put into a companion object, so it gets none either.
    Suppressed.<!UNRESOLVED_REFERENCE!>builder<!>()
    Suppressed.<!UNRESOLVED_REFERENCE!>Companion<!>

    // Everything else the annotation generates is untouched.
    val builder: Suppressed.SuppressedBuilder = instance.toBuilder()
    builder.x(1).build()
}

fun testRenamed() {
    val builder: Renamed.RenamedBuilder = Renamed.createBuilder()
    builder.x(1).build()
}
