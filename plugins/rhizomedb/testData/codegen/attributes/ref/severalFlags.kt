package test

import com.jetbrains.rhizomedb.*

class MyEntity(override val eid: EID) : Entity {
    @RefAttribute(RefFlags.CASCADE_DELETE, RefFlags.CASCADE_DELETE_BY)
    val parent: MyEntity by parentAttr

    companion object : EntityType<MyEntity>(MyEntity::class, ::MyEntity)
}
