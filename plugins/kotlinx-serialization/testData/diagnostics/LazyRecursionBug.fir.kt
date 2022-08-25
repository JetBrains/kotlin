// This test enshures that analysis ends up without compiler exceptions

import kotlinx.serialization.*

@Serializable
class Digest() {
    @Serializer(forClass = Digest::class)
    companion <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>object<!> : KSerializer<Digest> {}
}
