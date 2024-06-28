// CORRECT_ERROR_TYPES
// FILE: resolved.kt
import kotlin.io.encoding.*

var base64: Base64? = null

// FILE: unresolved.kt
@file:Suppress("UNRESOLVED_REFERENCE")
import kotlin.io.encoding.*

var base65: Base65? = null
