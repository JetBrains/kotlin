// FIR_IDENTICAL
// This test enshures that analysis ends up without compiler exceptions

import kotlinx.serialization.*

<!COMPANION_OBJECT_AS_CUSTOM_SERIALIZER_DEPRECATED!>@Serializable<!>
class Digest() {
    @Serializer(forClass = Digest::class)
    companion object : KSerializer<Digest> {}
}
