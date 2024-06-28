// FIR_IDENTICAL
// WITH_STDLIB
// SKIP_TXT

import kotlinx.serialization.*

@MetaSerializable
annotation class TopLevel

class MetaSerializableNestedTest {
    <!META_SERIALIZABLE_NOT_APPLICABLE!>@MetaSerializable<!>
    @Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
    annotation class JsonComment(val comment: String)

    object Nested2 {
        <!META_SERIALIZABLE_NOT_APPLICABLE!>@MetaSerializable<!>
        annotation class Nested3
    }

    @JsonComment("class_comment")
    data class IntDataCommented(val i: Int)
}
