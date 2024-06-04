package test

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
class MyEntity(override val eid: EID) : Entity {
    // OPTIONAL_COMPANION
    companion object {
        val X = 42
    }
}
