import com.jetbrains.rhizomedb.*

@GeneratedEntityType
interface MyEntity : Entity {
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
