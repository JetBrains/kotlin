// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: test.kt
import kotlinx.serialization.*

<!PLUGIN_ERROR("@Serializable annotation is ignored because it is impossible to serialize automatically interfaces or enums. Provide serializer manually via e.g. companion object")!>@Serializable<!>
interface INonSerializable
