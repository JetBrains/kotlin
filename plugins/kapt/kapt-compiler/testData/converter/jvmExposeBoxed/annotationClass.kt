// WITH_STDLIB

// Unsigned types are valid annotation member types, so an annotation class really can carry a value class.
// An annotation method must keep its declared name and its erased return type, so nothing here may be
// mangled and the stub has to stay a valid Java annotation declaration.

@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)

@JvmExposeBoxed
annotation class Unsigned(val count: UInt, vararg val more: UInt)

@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

@Unsigned(1u, 2u)
@JvmExposeBoxed
class Annotated(val id: Id)
