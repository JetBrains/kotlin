import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity

fun foo() {
    MyEntity.all()
    MyEntity.single()
    MyEntity.singleOrNull()
}