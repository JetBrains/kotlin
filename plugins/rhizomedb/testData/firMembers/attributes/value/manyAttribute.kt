// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    @Many
    @ValueAttribute
    val manyStrings: Set<String> by manyStringsAttr
}

fun foo(): Set<String> {
    val entity = entity(MyEntity.manyStringsAttr, "Hello") ?: return emptySet()
    val result = entity[MyEntity.manyStringsAttr]
    return result
}
