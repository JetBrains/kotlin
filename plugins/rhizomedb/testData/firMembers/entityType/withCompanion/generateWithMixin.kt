import com.jetbrains.rhizomedb.*
import kotlin.reflect.KClass

object MyEntityMixin : Mixin<MyEntity>(MyEntity::class)

@GeneratedEntityType(MyEntityMixin::class)
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