// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
data class Foo<T>(val i: Int, val t: T? = null)

@Serializable
class Holder(val f1: Foo<String>, val f2: Foo<Int>)


// tests for the issue from https://github.com/Kotlin/kotlinx.serialization/issues/2953#issuecomment-2735350353
@Serializable
sealed interface ParametrizedInterface<T>
{
    val value: T
}

@Serializable
data class Value<T>(override val value: T): ParametrizedInterface<T>

@Serializable
data class ParametrizedHolder<V>(val value: ParametrizedInterface<V>)

// mix type arguments, type parameter and simple type
@Serializable
sealed interface ParametrizedInterface2<T, K>
{
    val value: T
    val value2: K
}

@Serializable
data class Value2<T, K>(override val value: T, override val value2: K): ParametrizedInterface2<T, K>

@Serializable
data class ParametrizedHolder2<V>(val value: ParametrizedInterface2<V, Int>)

// all types are explicit
@Serializable
data class ParametrizedHolder0(val value1: ParametrizedInterface<Int>, val value2: ParametrizedInterface2<Int, Long>)


fun box(): String {
    val holder = Holder(Foo(1, "1"), Foo(2))
    val str = Json.encodeToString(Holder.serializer(), holder)
    if (str != """{"f1":{"i":1,"t":"1"},"f2":{"i":2}}""") return str
    val decoded = Json.decodeFromString(Holder.serializer(), str)
    if (decoded.f1.t != holder.f1.t) return "f1.t: ${decoded.f1.t}"

    assert("""{"value":{"type":"Value","value":1}}""") {
        Json.encodeToString(ParametrizedHolder(Value(1)))
    }?.let { return it }

    assert("""{"value":{"type":"Value2","value":1,"value2":2}}""") {
        Json.encodeToString(ParametrizedHolder2(Value2(1, 2)))
    }?.let { return it }

    assert("""{"value1":{"type":"Value","value":1},"value2":{"type":"Value2","value":1,"value2":2}}""") {
        Json.encodeToString(ParametrizedHolder0(Value(1), Value2(1, 2L)))
    }?.let { return it }

    return "OK"
}

private fun assert(expected: String, block: () -> String): String? {
    try {
        val actual = block()
        if (expected != actual) return actual
        return null
    } catch (e: Exception) {
        return e.message
    }
}
