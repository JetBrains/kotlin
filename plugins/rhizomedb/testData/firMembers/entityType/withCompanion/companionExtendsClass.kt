import com.jetbrains.rhizomedb.*

abstract class MyBaseClass(val id: String)

<!WRONG_ENTITY_TYPE_TARGET("target companion already extends a class")!>@GeneratedEntityType<!>
data class MyEntity(override val eid: EID) : Entity {
    companion object : MyBaseClass("MyEntity") {
        val X = 42
    }
}

fun foo() {
    MyEntity.<!NONE_APPLICABLE!>all<!>()
    MyEntity.<!NONE_APPLICABLE!>single<!>()
    MyEntity.<!NONE_APPLICABLE!>singleOrNull<!>()
}
