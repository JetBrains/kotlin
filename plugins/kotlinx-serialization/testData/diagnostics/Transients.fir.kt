// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE
// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*

@Serializable
data class WithTransients(@Transient val missing: Int) {
    @Transient val redundant: Int get() = 42

    @Transient
    lateinit var allowTransientLateinitWithoutInitializer: String
}