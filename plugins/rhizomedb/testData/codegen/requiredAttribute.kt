// CURIOUS_ABOUT: getStringAttr, foo, access$getStringAttr-field$cp, <init>, <clinit>
// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    @ValueAttribute
    val string: String = ""
}

fun foo(): String {
    val entity = entity(MyEntity.stringAttr, "Hello") ?: error("No Entity")
    val result = entity[MyEntity.stringAttr]
    return result
}
