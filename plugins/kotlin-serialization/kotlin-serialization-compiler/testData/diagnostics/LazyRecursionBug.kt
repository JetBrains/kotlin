// This test enshures that analysis ends up without compiler exceptions
// !DIAGNOSTICS: -OPT_IN_USAGE

import kotlinx.serialization.*

@Serializable
class Digest() {
    @Serializer(forClass = Digest::class)
    companion object : KSerializer<Digest> {}
}
