package test

import com.jetbrains.rhizomedb.*

interface MarkerInterface

class MyEntity(override val eid: EID) : Entity {
    @TransientAttribute(Indexing.UNIQUE)
    val marker: MarkerInterface by markerAttr

    companion object : EntityType<MyEntity>(MyEntity::class, ::MyEntity)
}
