// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    @Many
    @RefAttribute
    val children: Set<MyEntity> by childrenAttr
}

fun foo(child: MyEntity): Set<MyEntity> {
    val entity = entity(MyEntity.childrenAttr, child) ?: return emptySet()
    val result = entity[MyEntity.childrenAttr]
    return result
}
