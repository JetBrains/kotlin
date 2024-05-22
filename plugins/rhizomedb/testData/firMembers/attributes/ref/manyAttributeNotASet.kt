// WITH_STDLIB
import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    <!MANY_NON_SET!>@Many<!>
    <!WRONG_ATTRIBUTE_TARGET("reference attribute should be an Entity")!>@RefAttribute<!>
    val children: List<MyEntity> by <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>childrenAttr<!>

    <!MANY_NON_SET!>@Many<!>
    <!WRONG_ATTRIBUTE_TARGET("reference attribute should be an Entity")!>@RefAttribute<!>
    val childrenSet: MutableSet<MyEntity> by <!DELEGATE_SPECIAL_FUNCTION_RETURN_TYPE_MISMATCH!>childrenSetAttr<!>
}
