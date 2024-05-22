import com.jetbrains.rhizomedb.*

<!WRONG_ENTITY_TYPE_TARGET("target should be a regular class")!>@GeneratedEntityType<!>
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
    MyEntity.all()
    MyEntity.single()
    MyEntity.singleOrNull()
}
