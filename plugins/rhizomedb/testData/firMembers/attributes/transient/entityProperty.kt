// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    <!WRONG_ATTRIBUTE_TARGET("Entity property can be marked with @RefAttribute only")!>@TransientAttribute<!>
    val parent: MyEntity by parentAttr
}
