// TARGET_BACKEND: JS_IR
// ENABLE_SERIALIZATION
// WITH_STDLIB

package generatedCompanion

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.plugin.sandbox.AddToCompanion
import kotlin.test.assertEquals
import kotlin.test.assertTrue

interface SerializerApi {
    fun serializer(): String = "OK"
}

@AddToCompanion("generatedCompanion/SerializerApi")
@Serializable
class Owner(val name: String)

fun box(): String {
    val api: SerializerApi = Owner.Companion
    assertEquals("""{"name":"x"}""", Json.encodeToString(Owner.serializer(), Owner("x")))
    val apiResult: Any = api.serializer()
    // `SerializerApi.serializer()` runs the generated `serializer()` and returns a `KSerializer`.
    assertTrue(apiResult is KSerializer<*>, "Unexpected result: $apiResult")
    return "OK"
}
