/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
    OpenRanges,
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
    val capacityUnsigned by lazy { descendingByDomainCapacityUnsigned.indexOf(this).let { if (it < 0) it else descendingByDomainCapacityUnsigned.size - it } }

    companion object {
        val unsignedPrimitives = setOf(UInt, ULong, UByte, UShort)
        val defaultPrimitives = PrimitiveType.entries.toSet() - unsignedPrimitives
        val numericPrimitives = setOf(Int, Long, Byte, Short, Double, Float)
        val integralPrimitives = setOf(Int, Long, Byte, Short, Char)
        val floatingPointPrimitives = setOf(Double, Float)
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
fun PrimitiveType.isFloatingPoint(): Boolean = this in PrimitiveType.floatingPointPrimitives
fun PrimitiveType.isUnsigned(): Boolean = this in PrimitiveType.unsignedPrimitives

fun PrimitiveType.sumType() = when (this) {
    PrimitiveType.Byte, PrimitiveType.Short, PrimitiveType.Char -> PrimitiveType.Int
    PrimitiveType.UByte, PrimitiveType.UShort -> PrimitiveType.UInt
    else -> this
}

fun PrimitiveType.zero() = when (this) {
    PrimitiveType.Double -> "0.0"
    PrimitiveType.Float -> "0.0f"
    PrimitiveType.Long -> "0L"
    PrimitiveType.ULong -> "0uL"
    in PrimitiveType.unsignedPrimitives -> "0u"
    else -> "0"
}

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
    Native,
}

enum class Backend {
    Any,
    IR,
    Wasm,
}

enum class KotlinTarget(val platform: Platform, val backend: Backend) {
    Common(Platform.Common, Backend.Any),
    JVM(Platform.JVM, Backend.Any),
    JS(Platform.JS, Backend.IR),
    WASM(Platform.Native, Backend.Wasm),
    Native(Platform.Native, Backend.IR);

    val fullName get() = "Kotlin/$name"
}

enum class SequenceClass {
    terminal,
    intermediate,
    stateless,
    stateful
}

data class Deprecation(
    val message: String, val replaceWith: String? = null, val level: DeprecationLevel = DeprecationLevel.WARNING,
    val warningSince: String? = null, val errorSince: String? = null, val hiddenSince: String? = null)
val forBinaryCompatibility = Deprecation("Provided for binary compatibility", level = DeprecationLevel.HIDDEN)

data class ThrowsException(val exceptionType: String, val reason: String)

fun String.ifOrEmpty(condition: Boolean): String = if (condition) this else ""