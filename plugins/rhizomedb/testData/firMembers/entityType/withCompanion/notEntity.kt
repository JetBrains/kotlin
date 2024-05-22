import com.jetbrains.rhizomedb.*

interface BaseInterface

interface DerivedInterface : BaseInterface

<!WRONG_ENTITY_TYPE_TARGET("target should be an Entity")!>@GeneratedEntityType<!>
data class MyEntity(val eid: EID) : DerivedInterface {
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
