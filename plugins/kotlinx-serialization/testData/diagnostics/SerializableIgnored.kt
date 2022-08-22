// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE
// SKIP_TXT

// FILE: test.kt
import kotlinx.serialization.*

<!SERIALIZABLE_ANNOTATION_IGNORED!>@Serializable<!>
interface INonSerializable

@Serializable
sealed interface SealedSerializable
