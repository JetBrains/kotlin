package test

import com.jetbrains.rhizomedb.*

class MyEntity(override val eid: EID) : Entity {
    @ValueAttribute
    val strSet: Set<String> by strSetAttr

    companion object : EntityType<MyEntity>(MyEntity::class, ::MyEntity)
}
