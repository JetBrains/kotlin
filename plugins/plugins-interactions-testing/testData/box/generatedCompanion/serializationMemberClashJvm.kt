// TARGET_BACKEND: JVM
// ENABLE_SERIALIZATION
// WITH_STDLIB
// FULL_JDK

package generatedCompanion

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

interface SerializerApi {
    fun serializer(): String = "OK"
}

@AddToCompanion("generatedCompanion/SerializerApi")
@Serializable
class Owner(val name: String)

fun box(): String {
    val api: SerializerApi = Owner.Companion
    assertEquals("""{"name":"x"}""", Json.encodeToString(Owner.serializer(), Owner("x")))
    // `SerializerApi.serializer()` calls a bridge that casts the generated `KSerializer` to `String`.
    assertFailsWith<ClassCastException> { api.serializer() }
    return "OK"
}
