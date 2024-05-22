// WITH_STDLIB
import com.jetbrains.rhizomedb.*

interface MarkerInterface

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    <!MANY_NON_SET!>@Many<!>
    @TransientAttribute
    val manyMarkers: List<MarkerInterface> by <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>manyMarkersAttr<!>

    <!MANY_NON_SET!>@Many<!>
    @TransientAttribute
    val manyMutableMarkers: MutableSet<MarkerInterface> by <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>manyMutableMarkersAttr<!>
}
