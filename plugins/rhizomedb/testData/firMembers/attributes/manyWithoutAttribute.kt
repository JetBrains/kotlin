// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    <!NON_ATTRIBUTE!>@Many<!>
    val string: Set<String> by <!UNRESOLVED_REFERENCE!>stringAttr<!>
}
