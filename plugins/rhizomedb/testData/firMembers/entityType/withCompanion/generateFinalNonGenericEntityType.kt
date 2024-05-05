import com.jetbrains.rhizomedb.*

class MyEntityType : EntityType<MyEntity>("myEntity", ::MyEntity)

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