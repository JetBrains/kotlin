// FIR_IDENTICAL
// WITH_STDLIB
// FILE: test.kt
import kotlinx.serialization.*

@Serializable
data class WithTransients(<!TRANSIENT_MISSING_INITIALIZER!>@Transient val missing: Int<!>) {
    <!TRANSIENT_IS_REDUNDANT!>@Transient<!> val redundant: Int get() = 42

    @Transient
    lateinit var allowTransientLateinitWithoutInitializer: String
}
