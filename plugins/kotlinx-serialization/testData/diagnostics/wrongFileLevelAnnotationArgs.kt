// WITH_STDLIB

// The only K1 vs K2 difference is (ARGUMENT)_TYPE_MISMATCH diagnostic name

@file:UseSerializers(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>S<!>::class<!>, <!TYPE_MISMATCH!>String::class<!>)
@file:UseContextualSerialization(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>S<!>::class<!>, String::class)

import kotlinx.serialization.*

@Serializable
class X(val s: Int, @Contextual val d: String)
