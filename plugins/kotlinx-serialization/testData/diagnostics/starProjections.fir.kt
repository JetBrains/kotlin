// WITH_STDLIB
// SKIP_TXT

import kotlinx.serialization.*

@Serializable
class Box<T>(val boxed: T)

@Serializable
class Wrapper(val boxed: Box<<!SERIALIZER_NOT_FOUND!>*<!>>)
