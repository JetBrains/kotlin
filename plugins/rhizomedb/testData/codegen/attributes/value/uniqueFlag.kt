package test

import com.jetbrains.rhizomedb.*

class MyEntity(override val eid: EID) : Entity {
    @ValueAttribute(Indexing.UNIQUE)
    val str: String by strAttr

    companion object : EntityType<MyEntity>(MyEntity::class, ::MyEntity)
}
