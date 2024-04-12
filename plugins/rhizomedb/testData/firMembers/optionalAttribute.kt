// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>@Attribute
    val nullableString: String?<!>
}

fun foo(): String? {
    val entity = entity(MyEntity.nullableStringAttr, "Hello") ?: return null
    val result = entity[MyEntity.nullableStringAttr]
    return result
}