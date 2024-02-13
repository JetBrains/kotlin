// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
class SomeClass(val ctor: Int) {
    var body: String = ""

    // Not serializable: no backing field
    val getter: Int get() = 42
}

fun test(targetString: String): String {
    val c = SomeClass(1).apply { body = "x" }
    val s = Json.encodeToString(SomeClass.serializer(), c)
    if (s != targetString) return s
    val i = Json.decodeFromString(SomeClass.serializer(), s)
    if (i.ctor != c.ctor) return "Incorrect ctor"
    if (i.body != c.body) return "Incorrect body"
    return "OK"
}

fun box(): String {
    return test("""{"ctor":1,"body":"x"}""")
}
