// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    @ValueAttribute
    val strings: Set<String> by stringsAttr
}

fun foo(set: Set<String>): Set<String> {
    val entity = entity(MyEntity.stringsAttr, set) ?: error("No Entity")
    val result = entity[MyEntity.stringsAttr]
    return result
}