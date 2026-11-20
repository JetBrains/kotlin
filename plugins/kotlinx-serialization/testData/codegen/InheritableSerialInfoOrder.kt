// CURIOUS_ABOUT: <clinit>
// WITH_STDLIB
// KT-91576: an inheritable serial info annotation that several supertypes have is taken from the first of them
// in the depth-first order of the declared supertypes, not in the order of their hash codes.

import kotlinx.serialization.*

@InheritableSerialInfo
annotation class Tag(val value: String)

@Tag("a")
interface A

@Tag("a")
interface B

@Tag("a")
interface C

@Tag("a")
interface D

@Tag("a")
abstract class Base : A

@Serializable
class Impl : Base(), B, C, D
