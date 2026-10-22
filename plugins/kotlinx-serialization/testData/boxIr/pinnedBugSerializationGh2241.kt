// TARGET_BACKEND: JVM
// WITH_STDLIB
// EXPECTED_COMPILER_CRASH: java.lang.IndexOutOfBoundsException: Index 0 out of bounds for length 0
// EXPECTED_COMPILER_CRASH: formEncodeDecodePropertyCall

// Pinned bug test: the compiler is expected to throw until the issue is fixed.
// Monitors if further changes will affect the current behaviour. Can be redone
// into valid test, when the issue is fixed.
// ISSUE: https://github.com/Kotlin/kotlinx.serialization/issues/2241
// Private property in a generic serializable abstract class crashes the plugin
// when inherited through a chain of subclasses.

import kotlinx.serialization.*
import kotlinx.serialization.json.*

interface Entity {
    val id: Int
}

@Serializable
data class Item(override val id: Int) : Entity

@Serializable
abstract class Parent<E : Entity>(
    @Required private val entities: MutableMap<Int, E> = mutableMapOf()
)

@Serializable
abstract class Child<E : Entity> : Parent<E>() {
    abstract val elementSerializer: KSerializer<E>
}

@Serializable
class GrandChild : Child<Item>() {
    @Transient
    override val elementSerializer = Item.serializer()
}

fun box(): String {
    val encoded = Json.encodeToString(GrandChild())
    if (encoded != """{"entities":{}}""") return "Fail: $encoded"
    return "OK"
}
