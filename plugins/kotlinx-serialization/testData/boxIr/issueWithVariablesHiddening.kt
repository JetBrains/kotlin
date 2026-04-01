// WITH_STDLIB
// https://github.com/Kotlin/kotlinx.serialization/issues/2523

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlin.test.assertEquals

@Serializable
data class Model(val abc: String?) {
    val xyz = abc?.let { xyz -> "$xyz..." }
}

fun box(): String {
    val jsonObject = buildJsonObject { put("abc", "hello") }
    val model = Json.decodeFromJsonElement<Model>(jsonObject)

    assertEquals("hello", model.abc)
    assertEquals("hello...", model.xyz)

    return "OK"
}
