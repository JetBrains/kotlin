import com.jetbrains.rhizomedb.*

<!WRONG_ENTITY_TYPE_TARGET("target should be a regular class")!>@GeneratedEntityType<!>
interface MyEntity : Entity {
    // OPTIONAL_COMPANION
    companion object {
        val X = 42
    }
}

fun foo() {
    MyEntity.all()
    MyEntity.single()
    MyEntity.singleOrNull()
}
