// WITH_STDLIB

import com.jetbrains.rhizomedb.*

interface MarkerInterface

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    @TransientAttribute
    val markers: Set<MarkerInterface> by markersAttr
}

fun foo(markers: Set<MarkerInterface>): Set<MarkerInterface> {
    val entity = entity(MyEntity.markersAttr, markers) ?: error("No Entity")
    val result = entity[MyEntity.markersAttr]
    return result
}