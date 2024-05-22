// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    @ValueAttribute
    val nullableString: String? by nullableStringAttr
}

fun foo(): String? {
    val entity = entity(MyEntity.nullableStringAttr, "Hello") ?: return null
    val result = entity[MyEntity.nullableStringAttr]
    return result
}