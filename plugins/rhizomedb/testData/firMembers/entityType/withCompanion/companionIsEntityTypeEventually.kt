import com.jetbrains.rhizomedb.*

abstract class MyEntityType(
    ident: String,
    cons: (EID) -> MyEntity
) : EntityType<MyEntity>(ident, cons)

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    companion object : MyEntityType("MyEntity", ::MyEntity) {
        val X = 42
    }
}

fun foo() {
    MyEntity.all()
    MyEntity.single()
    MyEntity.singleOrNull()
}
