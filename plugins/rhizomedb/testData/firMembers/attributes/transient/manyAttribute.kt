// WITH_STDLIB

import com.jetbrains.rhizomedb.*

interface MarkerInterface

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    @Many
    @TransientAttribute
    val manyMarkers: Set<MarkerInterface> by manyMarkersAttr
}

fun foo(marker: MarkerInterface): Set<MarkerInterface> {
    val entity = entity(MyEntity.manyMarkersAttr, marker) ?: return emptySet()
    val result = entity[MyEntity.manyMarkersAttr]
    return result
}
