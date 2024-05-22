// WITH_STDLIB

import com.jetbrains.rhizomedb.*

interface MarkerInterface

data class MyEntity(override val eid: EID) : Entity {
    <!WRONG_ATTRIBUTE_TARGET("no EntityType as companion found")!>@ValueAttribute<!>
    val string: String by <!UNRESOLVED_REFERENCE!>stringAttr<!>

    <!WRONG_ATTRIBUTE_TARGET("no EntityType as companion found")!>@TransientAttribute<!>
    val serializer: <!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>KSerializer<!><String> by <!UNRESOLVED_REFERENCE!>serializerAttr<!>

    <!WRONG_ATTRIBUTE_TARGET("no EntityType as companion found")!>@RefAttribute<!>
    val entity: Entity by <!UNRESOLVED_REFERENCE!>entityAttr<!>

    companion object : MarkerInterface {
        val X = 42
    }
}
