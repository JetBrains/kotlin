package test

import com.jetbrains.rhizomedb.*

interface MarkerInterface

class MyEntity(override val eid: EID) : Entity {
    @TransientAttribute
    val markerSet: Set<MarkerInterface> by markerSetAttr

    companion object : EntityType<MyEntity>(MyEntity::class, ::MyEntity)
}
