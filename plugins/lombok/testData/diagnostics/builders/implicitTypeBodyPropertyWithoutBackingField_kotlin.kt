// ISSUE: KT-90508
// WITH_STDLIB
import lombok.Builder
import kotlin.properties.Delegates

@Builder
class Entity {
    var explicitDelegated: Int by Delegates.notNull()

    // Implicit type, error should be reported
    var <!BUILDER_PROPERTY_NOT_INCLUDED_ERROR!>implicitDelegated<!> by Delegates.notNull<Int>()

    var explicitAccessors: Int
        get() = 1
        set(value) {}

    // Implicit type, error should be reported
    var <!BUILDER_PROPERTY_NOT_INCLUDED_ERROR!>implicitAccessors<!>
        get() = 1
        set(value) {}
}

fun usage() {
    Entity.builder()
        .explicitDelegated(1)
        .<!UNRESOLVED_REFERENCE!>implicitDelegated<!>(2)
        .explicitAccessors(3)
        .implicitAccessors(4)
        .build()
}
