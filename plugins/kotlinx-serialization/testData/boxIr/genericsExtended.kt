// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed interface Interface<out T>

@Serializable
data class Child<out T>(val x: T) : Interface<T>

@Serializable
data class ChildWithUnused<UNUSED1, UNUSED2, out T>(val x: T) : Interface<T>

@Serializable
data class Container<out T>(val x: List<Interface<T>>)

@Serializable
data class ContainerWithUnused<UNUSED, out T>(val x: List<Interface<T>>)

@Serializable
open class OpenClass<T>(
    val value: Interface<T>
)

@Serializable
class IndirectClass<A, B>(val a: A, val b: B) : OpenClass<B>(Child(b))

@Serializable data object SerializableObject

@Serializable
sealed interface AnotherInterface<T> {
    @Serializable data class AnotherChild<T>(val t: T) : AnotherInterface<T>
}

@Serializable
data class MapContainer(
    val choices: Map<Int, AnotherInterface<SerializableObject>>,
)

fun box(): String {
    val child: Interface<Int> = Child(42)
    val container: Container<Int> = Container(listOf(child))

    val json1 = Json.encodeToString(container)
    if (json1 != """{"x":[{"type":"Child","x":42}]}""") return json1
    Json.decodeFromString<Container<Int>>(json1)

    val childWithUnused: Interface<Int> = ChildWithUnused<Long, String, Int>(42)
    val containerWithUnused: ContainerWithUnused<Long, Int> = ContainerWithUnused(listOf(childWithUnused))
    val json2 = Json.encodeToString(containerWithUnused)
    if (json2 != """{"x":[{"type":"ChildWithUnused","x":42}]}""") return json2
    Json.decodeFromString<ContainerWithUnused<Long, Int>>(json2)


    val json3 = Json.encodeToString(MapContainer(mapOf(0 to AnotherInterface.AnotherChild(SerializableObject))))
    if (json3 != """{"choices":{"0":{"type":"AnotherInterface.AnotherChild","t":{}}}}""") return json3
    Json.decodeFromString<MapContainer>(json3)

    val json4 = Json.encodeToString(IndirectClass(42, "foo"))
    if (json4 != """{"value":{"type":"Child","x":"foo"},"a":42,"b":"foo"}""") return json4
    Json.decodeFromString<IndirectClass<Int, String>>(json4)

    return "OK"
}
