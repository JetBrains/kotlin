// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT
import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    <!MANY_ATTRIBUTE_NOT_A_SET!>@Many
    @ValueAttribute
    val manyStringsList: List<String> by <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>manyStringsListAttr<!><!>

    @Many
    @ValueAttribute
    val manyStringsSet: Set<String> by manyStringsSetAttr
}
