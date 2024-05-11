import com.jetbrains.rhizomedb.*

@GeneratedEntityType
enum class MyEntity(override val eid: EID) : Entity {
    <!WRONG_ANNOTATION_TARGET!>@GeneratedEntityType<!>
    A(0),
    B(1);

    // OPTIONAL_COMPANION
    companion object {
        val X = 42
    }
}

fun foo() {
    MyEntity.<!NONE_APPLICABLE!>all<!>()
    MyEntity.<!NONE_APPLICABLE!>single<!>()
    MyEntity.<!NONE_APPLICABLE!>singleOrNull<!>()
}
