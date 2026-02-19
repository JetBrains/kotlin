// WITH_STDLIB
// SKIP_TXT

import kotlinx.serialization.*

@Serializable
class Box<T>(val boxed: T)

@Serializable
class Wrapper(<!SERIALIZER_NOT_FOUND!>val boxed: Box<*><!>)
