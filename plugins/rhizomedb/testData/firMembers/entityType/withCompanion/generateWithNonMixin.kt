import com.jetbrains.rhizomedb.*
import kotlin.reflect.KClass

@GeneratedEntityType(<!ARGUMENT_TYPE_MISMATCH!>String::class<!>)
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
