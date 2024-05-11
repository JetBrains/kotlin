import com.jetbrains.rhizomedb.*
import kotlin.reflect.KClass

abstract class MyEntityMixin<E : Entity>(ident: String) : Mixin<E>(ident)

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
