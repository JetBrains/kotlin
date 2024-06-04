package test

import com.jetbrains.rhizomedb.*

interface MarkerInterface

@GeneratedEntityType
class MyEntity(override val eid: EID) : Entity {
    @TransientAttribute(Indexing.UNIQUE)
    val marker: MarkerInterface by markerAttr

    @ValueAttribute
    val str: String? by strAttr

    @Many
    @RefAttribute
    val parents: Set<MyEntity> by parentsAttr

    companion object {
        val X = 42
    }
}
