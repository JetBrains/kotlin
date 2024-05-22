// WITH_STDLIB

import com.jetbrains.rhizomedb.*

data class MyEntity(override val eid: EID) : Entity {
    @ValueAttribute
    val string: String by stringAttr
    val int: Int by intAttr

    companion object : EntityType<MyEntity>(MyEntity::class, ::MyEntity) {
        private val intAttr = requiredTransient<Int>("int")
    }
}

fun foo(): String {
    val entity = entity(MyEntity.stringAttr, "Hello") ?: error("No Entity")
    val result = entity[MyEntity.stringAttr]
    return result
}