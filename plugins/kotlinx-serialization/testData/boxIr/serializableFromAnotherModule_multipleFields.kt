// TARGET_BACKEND: JVM_IR
// WITH_STDLIB
// ISSUE: KT-57626

// MODULE: lib
import kotlinx.serialization.*

@Serializable
open class Base(
    val c: Int = 1,
    val b: String = "hello",
    val a: List<String> = listOf("a")
)

// MODULE: main(lib)
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class Derived(val d: Long = 1000000000000) : Base(2, "world", listOf("b", "c")) {
    override fun equals(other: Any?): Boolean {
        if (other !is Derived) return false
        return a == other.a && b == other.b && c == other.c && d == other.d
    }

    override fun toString(): String {
        return "a: $a, b: $b, c: $c, d: $d"
    }
}

fun box(): String {
    val expected = Derived(12)
    val result = Json.encodeToString(Derived.serializer(), expected)
    if (result != """{"c":2,"b":"world","a":["b","c"],"d":12}""") {
        return "Fail: $result"
    }
    val actual = Json.decodeFromString(Derived.serializer(), result)
    if (expected != actual) {
        return "Fail: expected: $expected\nactual: $actual"
    }
    return "OK"
}
