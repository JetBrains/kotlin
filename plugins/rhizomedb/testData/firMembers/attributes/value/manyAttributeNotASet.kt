// WITH_STDLIB
import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    <!MANY_NON_SET!>@Many<!>
    @ValueAttribute
    val manyStringsList: List<String> by <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>manyStringsListAttr<!>

    <!MANY_NON_SET!>@Many<!>
    @ValueAttribute
    val manyStrings: MutableSet<String> by <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>manyStringsAttr<!>
}
