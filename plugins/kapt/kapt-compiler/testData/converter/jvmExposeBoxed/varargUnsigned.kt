// WITH_STDLIB

// A user value class has no corresponding array type, so the only vararg *of* a value class is an unsigned
// one: 'vararg counts: UInt' has parameter type 'UIntArray', which is itself a '@JvmInline value class'. The
// declaration is therefore mangled, and only the boxed variant can survive stub generation.

@file:OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)

@JvmExposeBoxed
fun sumOf(vararg counts: UInt): UInt = counts.size.toUInt()

class Host {
    @JvmExposeBoxed
    fun member(vararg counts: UInt): UInt = counts.size.toUInt()
}
