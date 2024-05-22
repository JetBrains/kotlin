// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    @RefAttribute
    val nullableParent: MyEntity? by nullableParentAttr
}

fun foo(child: MyEntity): MyEntity? {
    val entity = entity(MyEntity.nullableParentAttr, child) ?: return null
    val result = entity[MyEntity.nullableParentAttr]
    return result
}