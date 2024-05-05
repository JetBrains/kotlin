import com.jetbrains.rhizomedb.*

interface BaseEntity : Entity

interface DerivedEntity : BaseEntity

@GeneratedEntityType
data class MyEntity(override val eid: EID) : DerivedEntity {
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
