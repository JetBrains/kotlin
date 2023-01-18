// TARGET_BACKEND: JVM_IR

// WITH_STDLIB

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import kotlinx.serialization.encoding.*

@Serializable
data class Holder<T>(
    val ok: Boolean,
    @Contextual
    val result: T?
)

fun box(): String {
    val serializer = serializer<Holder<List<String>>>()
    val instance = Holder(true, listOf("a", "b"))
    val encoded = Json.encodeToString(serializer, instance)
    if (encoded != """{"ok":true,"result":["a","b"]}""") return encoded
    return "OK"
}
