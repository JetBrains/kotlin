import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    companion object : EntityType<MyEntity>(MyEntity::class, ::MyEntity) {
        val X = 42
    }
}

fun foo() {
    MyEntity.all()
    MyEntity.single()
    MyEntity.singleOrNull()
}
