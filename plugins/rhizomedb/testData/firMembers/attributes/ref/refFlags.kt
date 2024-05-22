// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    @RefAttribute(RefFlags.CASCADE_DELETE_BY)
    val parent: MyEntity by parentAttr
}

fun foo(parent: MyEntity): MyEntity {
    val entity = entity(MyEntity.parentAttr, parent) ?: error("No Entity")
    val result = entity[MyEntity.parentAttr]
    return result
}