package test

import com.jetbrains.rhizomedb.*

class MyEntity(override val eid: EID) : Entity {
    @Many
    @ValueAttribute
    val strs: Set<String> by strsAttr

    companion object : EntityType<MyEntity>(MyEntity::class, ::MyEntity)
}
