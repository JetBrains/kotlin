package test

import com.jetbrains.rhizomedb.*

interface MarkerInterface

class MyEntity(override val eid: EID) : Entity {
    @Many
    @TransientAttribute
    val markers: Set<MarkerInterface> by markersAttr

    companion object : EntityType<MyEntity>(MyEntity::class, ::MyEntity)
}
