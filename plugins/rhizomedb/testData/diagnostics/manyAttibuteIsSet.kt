// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT
import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    <!MANY_ATTRIBUTE_NOT_A_SET, MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>@Many
    @Attribute
    val manyStringsList: List<String><!>

    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>@Many
    @Attribute
    val manyStringsSet: Set<String><!>
}
