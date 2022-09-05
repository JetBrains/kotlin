// This test enshures that analysis ends up without compiler exceptions
// !DIAGNOSTICS: -OPT_IN_USAGE

import kotlinx.serialization.*

@Serializable
class Digest() {
    @<!OPT_IN_USAGE_ERROR!>Serializer<!>(forClass = Digest::class)
    companion <!ABSTRACT_MEMBER_NOT_IMPLEMENTED!>object<!> : KSerializer<Digest> {}
}
