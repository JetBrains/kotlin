// WITH_STDLIB

import com.jetbrains.rhizomedb.*

interface MarkerInterface

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    <!WRONG_ATTRIBUTE_TARGET("property may be marked with single attribute annotation only")!>@ValueAttribute<!>
    <!WRONG_ATTRIBUTE_TARGET("property may be marked with single attribute annotation only")!>@TransientAttribute<!>
    val string: String by stringAttr
}
