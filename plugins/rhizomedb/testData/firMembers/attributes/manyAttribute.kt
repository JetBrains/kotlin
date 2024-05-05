// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType(EntityType::class)
data class MyEntity(override val eid: EID) : Entity {
    @Many
    @ValueAttribute
    val manyStrings: Set<String> by manyStringsAttr

    companion object {
        const val x = 10
    }
}

fun foo(): Set<String> {
    val entity = entity(MyEntity.manyStringsAttr, "Hello") ?: return emptySet()
    val result = entity[MyEntity.manyStringsAttr]
    return result
}