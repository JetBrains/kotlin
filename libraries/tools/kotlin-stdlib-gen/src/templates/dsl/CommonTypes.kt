/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package templates

enum class Family {
    Iterables,
    Collections,
    Lists,
    Sets,
    Maps,
    InvariantArraysOfObjects,
    ArraysOfObjects,
    ArraysOfPrimitives,
    ArraysOfUnsigned,
    Sequences,
    CharSequences,
    Strings,
    Ranges,
    RangesOfPrimitives,
    ProgressionsOfPrimitives,
    Generic,
    Primitives,
    Unsigned;

    val isPrimitiveSpecialization: Boolean by lazy { this in primitiveSpecializations }

    class DocExtension(val family: Family)
    class CodeExtension(val family: Family)
    val doc = DocExtension(this)
    val code = CodeExtension(this)

    companion object {
        val primitiveSpecializations = setOf(ArraysOfPrimitives, RangesOfPrimitives, ProgressionsOfPrimitives, Primitives)
        val defaultFamilies = setOf(Iterables, Sequences, ArraysOfObjects, ArraysOfPrimitives)
    }
}

enum class PrimitiveType {
    Byte,
    Short,
    Int,
    Long,
    Float,
    Double,
    Boolean,
    Char,
    // unsigned
    UByte,
    UShort,
    UInt,
    ULong;

    val capacity by lazy { descendingByDomainCapacity.indexOf(this).let { if (it < 0) it else descendingByDomainCapacity.size - it } }

    companion object {
        val unsignedPrimitives = setOf(UInt, ULong, UByte, UShort)
        val defaultPrimitives = PrimitiveType.values().toSet() - unsignedPrimitives
        val numericPrimitives = setOf(Int, Long, Byte, Short, Double, Float)
        val integralPrimitives = setOf(Int, Long, Byte, Short, Char)
        val rangePrimitives = setOf(Int, Long, Char, UInt, ULong)

        val descendingByDomainCapacity = listOf(Double, Float, Long, Int, Short, Char, Byte)
        val descendingByDomainCapacityUnsigned = listOf(ULong, UInt, UShort, UByte)

        fun maxByCapacity(fromType: PrimitiveType, toType: PrimitiveType): PrimitiveType =
            (if (fromType in unsignedPrimitives) descendingByDomainCapacityUnsigned else descendingByDomainCapacity)
                .first { it == fromType || it == toType }
    }
}

fun PrimitiveType.isIntegral(): Boolean = this in PrimitiveType.integralPrimitives
fun PrimitiveType.isNumeric(): Boolean = this in PrimitiveType.numericPrimitives
enum class Inline {
    No,
    Yes,
    YesSuppressWarning,  // with suppressed warning about nothing to inline
    Only;

    fun isInline() = this != No
}

enum class Platform {
    Common,
    JVM,
    JS,
    Native;

    val fullName get() = "Kotlin/$name"

    companion object {
        val values = values().toList()
    }
}

enum class SequenceClass {
    terminal,
    intermediate,
    stateless,
    stateful
}

data class Deprecation(val message: String, val replaceWith: String? = null, val level: DeprecationLevel = DeprecationLevel.WARNING)
val forBinaryCompatibility = Deprecation("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)