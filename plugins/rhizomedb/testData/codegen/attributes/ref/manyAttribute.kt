package test

import com.jetbrains.rhizomedb.*

class MyEntity(override val eid: EID) : Entity {
    @Many
    @RefAttribute
    val parents: Set<MyEntity> by parentsAttr

    companion object : EntityType<MyEntity>(MyEntity::class, ::MyEntity)
}
