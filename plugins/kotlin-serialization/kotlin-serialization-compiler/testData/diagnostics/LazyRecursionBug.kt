// This test enshures that analysis ends up without compiler exceptions
// !DIAGNOSTICS: -EXPERIMENTAL_API_USAGE

import kotlinx.serialization.*

@Serializable
class Digest() {
    @Serializer(forClass = Digest::class)
    companion object : KSerializer<Digest> {}
}
