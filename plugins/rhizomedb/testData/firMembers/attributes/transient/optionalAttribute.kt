// WITH_STDLIB

import com.jetbrains.rhizomedb.*

interface MarkerInterface

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    @TransientAttribute
    val nullableMarker: MarkerInterface? by nullableMarkerAttr
}

fun foo(marker: MarkerInterface): MarkerInterface? {
    val entity = entity(MyEntity.nullableMarkerAttr, marker) ?: return null
    val result = entity[MyEntity.nullableMarkerAttr]
    return result
}