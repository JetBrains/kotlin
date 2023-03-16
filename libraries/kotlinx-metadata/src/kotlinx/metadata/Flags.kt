/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:JvmName("FlagsKt")

package kotlinx.metadata

/**
 * Declaration flags are represented as bitmasks of this type.
 *
 * @see Flag
 */
//typealias Flags = Int

// Can't be sealed/internal because of JvmFlags
open class Flags(val rawValue: Int) {
    fun <F: Flags> plus(f: F, ctor: (Int) -> F): F = ctor(rawValue + f.rawValue)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Flags

        if (rawValue != other.rawValue) return false

        return true
    }

    override fun hashCode(): Int {
        return rawValue
    }


}

class LegacyFlags(rawValue: Int) : Flags(rawValue)

sealed class DeclarationFlags(rawValue: Int) : Flags(rawValue)

class ClassFlags(rawValue: Int) : DeclarationFlags(rawValue) {
    constructor(vararg flags: ClassFlag): this(flagsOfImpl(*flags))

    companion object {
        val isData: ClassFlag = object : ClassFlag {
            override val f: Flag = Flag.Class.IS_DATA
        }
    }
}
class ConstructorFlags(rawValue: Int) : DeclarationFlags(rawValue) {

    constructor(vararg flags: ConstructorFlag): this(flagsOfImpl(*flags))
    operator fun plus(flags: ConstructorFlags): ConstructorFlags = plus(flags, ::ConstructorFlags)

    companion object {
        val hasNonStableParameterNames = object : ConstructorFlag {
            override val f: Flag = Flag(org.jetbrains.kotlin.metadata.deserialization.Flags.IS_CONSTRUCTOR_WITH_NON_STABLE_PARAMETER_NAMES)
        }
    }
}
class FunctionFlags(rawValue: Int) : DeclarationFlags(rawValue) {
    constructor(vararg flags: FunctionFlag): this(flagsOfImpl(*flags))
    operator fun plus(flags: FunctionFlags): FunctionFlags = plus(flags, ::FunctionFlags)

    companion object {
        val hasNonStableParameterNames = object : FunctionFlag {
            override val f: Flag = Flag(org.jetbrains.kotlin.metadata.deserialization.Flags.IS_FUNCTION_WITH_NON_STABLE_PARAMETER_NAMES)
        }
    }
}
class PropertyFlags(rawValue: Int) : DeclarationFlags(rawValue)
class PropertyAccessorFlags(rawValue: Int) : DeclarationFlags(rawValue)
class TypeFlags(rawValue: Int) : Flags(rawValue)
class TypeAliasFlags(rawValue: Int) : Flags(rawValue)
class TypeParameterFlags(rawValue: Int) : Flags(rawValue)
class ValueParameterFlags(rawValue: Int) : Flags(rawValue)

class EffectFlags(rawValue: Int) : Flags(rawValue)

// may be internal if copypasted to JvmFlag.kt
@Suppress("DEPRECATION_ERROR")
fun flagsOfImpl(vararg flags: FlagWrapper): Int = flags.fold(0) { acc: Int, flag ->  flag.f.plus(acc)}
/**
 * Combines several flags into an integer bitmask.
 *
 * Note that in case several mutually exclusive flags are passed (for example, several visibility flags), the resulting bitmask will
 * hold the value of the latest flag. For example, `flagsOf(Flag.IS_PRIVATE, Flag.IS_PUBLIC, Flag.IS_INTERNAL)` is the same as
 * `flagsOf(Flag.IS_INTERNAL)`.
 */
fun flagsOf(vararg flags: Flag): LegacyFlags =
    LegacyFlags(flags.fold(0) { acc: Int, flag: Flag ->
        @Suppress("DEPRECATION_ERROR")
        flag.plus(acc)
    })
