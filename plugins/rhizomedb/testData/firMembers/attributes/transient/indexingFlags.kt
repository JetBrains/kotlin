// WITH_STDLIB

import com.jetbrains.rhizomedb.*

interface MarkerInterface

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    @TransientAttribute(Indexing.UNIQUE)
    val marker: MarkerInterface by markerAttr
}

fun foo(marker: MarkerInterface): MarkerInterface {
    val entity = entity(MyEntity.markerAttr, marker) ?: error("No Entity")
    val result = entity[MyEntity.markerAttr]
    return result
}