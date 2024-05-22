// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    <!WRONG_ATTRIBUTE_TARGET("reference attribute should be an Entity")!>@RefAttribute<!>
    val str: String by strAttr

    <!WRONG_ATTRIBUTE_TARGET("reference attribute should be an Entity")!>@RefAttribute<!>
    val children: Set<MyEntity> by childrenAttr
}
