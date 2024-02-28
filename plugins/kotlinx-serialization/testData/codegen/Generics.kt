// CURIOUS_ABOUT: write$Self$main
// WITH_STDLIB

import kotlinx.serialization.*

@Serializable
class Box<T>(val boxedValue: T)

@Serializable
class BoxNotNull<T: Any>(val boxedValue: T)