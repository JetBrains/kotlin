// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>@Attribute
    val string: String<!>
}

fun foo(): String {
    val entity = entity(MyEntity.stringAttr, "Hello") ?: error("No Entity")
    val result = entity[MyEntity.stringAttr]
    return result
}