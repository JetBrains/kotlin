// WITH_STDLIB

import com.jetbrains.rhizomedb.*

@GeneratedEntityType
data class MyEntity(override val eid: EID) : Entity {
    <!MUST_BE_INITIALIZED_OR_BE_ABSTRACT!>@Many
    @Attribute
    val manyStrings: Set<String><!>
}

fun foo(): Set<String> {
    val entity = entity(MyEntity.manyStringsAttr, "Hello") ?: return emptySet()
    val result = entity[MyEntity.manyStringsAttr]
    return result
}