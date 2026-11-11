// RUN_PIPELINE_TILL: FRONTEND

// FILE: test.kt
import kotlinx.serialization.*

<!SERIALIZABLE_ANNOTATION_IGNORED!>@Serializable<!>
interface INonSerializable

@Serializable
sealed interface SealedSerializable

/* GENERATED_FIR_TAGS: interfaceDeclaration, sealed */
