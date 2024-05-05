import com.jetbrains.rhizomedb.*

abstract class MyEntityType(
    ident: String,
    cons: (EID) -> MyEntity
) : EntityType<MyEntity>(ident, cons)

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
