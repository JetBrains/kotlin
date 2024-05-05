import com.jetbrains.rhizomedb.*

abstract class MyEntityType<E : Entity>(
    ident: String,
    cons: (EID) -> E
) : EntityType<E>(ident, cons)

@GeneratedEntityType(MyEntityType::class)
data class MyEntity(override val eid: EID) : Entity {
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