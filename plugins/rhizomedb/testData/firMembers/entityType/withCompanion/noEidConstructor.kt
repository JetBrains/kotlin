import com.jetbrains.rhizomedb.*

<!WRONG_ENTITY_TYPE_TARGET("target should have from EID constructor")!>@GeneratedEntityType<!>
data class MyEntity(override val eid: EID, val data: Any) : Entity {
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
