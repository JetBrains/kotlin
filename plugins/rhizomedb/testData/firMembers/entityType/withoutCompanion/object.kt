import com.jetbrains.rhizomedb.*

<!WRONG_ENTITY_TYPE_TARGET("target should be a regular class")!>@GeneratedEntityType<!>
object MyEntity : Entity {
    override val eid: EID = 0
}

fun foo() {
    MyEntity.<!NONE_APPLICABLE!>all<!>()
    MyEntity.<!NONE_APPLICABLE!>single<!>()
    MyEntity.<!NONE_APPLICABLE!>singleOrNull<!>()
}
