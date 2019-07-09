// !DIAGNOSTICS: -UNUSED_PARAMETER,-UNUSED_VARIABLE

// FILE: test.kt
import kotlinx.serialization.*

@Serializable
data class WithTransients(<!PLUGIN_ERROR("This property is marked as @Transient and therefore must have an initializing expression")!>@Transient val missing: Int<!>) {
    <!PLUGIN_WARNING("Property does not have backing field which makes it non-serializable and therefore @Transient is redundant")!>@Transient<!> val redundant: Int get() = 42
}
