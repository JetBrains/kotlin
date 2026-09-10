/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.konan.target.Architecture
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * Pre-computed ABI data layout values for a target architecture.
 *
 * These values are deterministic per target and don't need to be queried from LLVM.
 * Pre-computing them eliminates JNI overhead for repeated LLVM calls like:
 * - LLVMABISizeOfType
 * - LLVMABIAlignmentOfType
 * - LLVMStoreSizeOfType
 * - LLVMByteOrder
 */
internal sealed class TargetDataLayout(
        val pointerSize: Int,
        val pointerAlignment: Int,
        val isBigEndian: Boolean = false, // All K/N targets are little-endian
        val objectAlignment: Int = 8
) {
    // Primitive type alignments (architecture-specific)
    abstract val int8Alignment: Int
    abstract val int16Alignment: Int
    abstract val int32Alignment: Int
    abstract val int64Alignment: Int
    abstract val floatAlignment: Int
    abstract val doubleAlignment: Int
    abstract val vector128Alignment: Int

    // i1 (boolean) alignment - same as int8 on all platforms
    val int1Alignment: Int get() = int8Alignment

    // Primitive type sizes are standard
    val int1Size: Int = 1 // i1 is stored as 1 byte
    val int8Size: Int = 1
    val int16Size: Int = 2
    val int32Size: Int = 4
    val int64Size: Int = 8
    val floatSize: Int = 4
    val doubleSize: Int = 8
    val vector128Size: Int = 16

    /**
     * Get the ABI alignment for a primitive binary type.
     */
    fun alignmentOf(type: PrimitiveBinaryType?): Int = when (type) {
        null -> pointerAlignment // Reference type
        PrimitiveBinaryType.BOOLEAN -> int1Alignment
        PrimitiveBinaryType.BYTE -> int8Alignment
        PrimitiveBinaryType.SHORT -> int16Alignment
        PrimitiveBinaryType.INT -> int32Alignment
        PrimitiveBinaryType.LONG -> int64Alignment
        PrimitiveBinaryType.FLOAT -> floatAlignment
        PrimitiveBinaryType.DOUBLE -> doubleAlignment
        PrimitiveBinaryType.VECTOR128 -> vector128Alignment
        PrimitiveBinaryType.POINTER -> pointerAlignment
    }

    /**
     * Get the ABI size (with padding) for a primitive binary type.
     */
    fun sizeOf(type: PrimitiveBinaryType?): Int = when (type) {
        null -> pointerSize // Reference type
        PrimitiveBinaryType.BOOLEAN -> int1Size
        PrimitiveBinaryType.BYTE -> int8Size
        PrimitiveBinaryType.SHORT -> int16Size
        PrimitiveBinaryType.INT -> int32Size
        PrimitiveBinaryType.LONG -> int64Size
        PrimitiveBinaryType.FLOAT -> floatSize
        PrimitiveBinaryType.DOUBLE -> doubleSize
        PrimitiveBinaryType.VECTOR128 -> vector128Size
        PrimitiveBinaryType.POINTER -> pointerSize
    }

    /**
     * Get the store size (without trailing padding) for a primitive binary type.
     * For primitive types, store size equals ABI size.
     */
    fun storeSizeOf(type: PrimitiveBinaryType?): Int = sizeOf(type)

    companion object {
        fun forTarget(target: KonanTarget): TargetDataLayout = when (target.architecture) {
            Architecture.X64 -> X64DataLayout
            Architecture.ARM64 -> ARM64DataLayout
            Architecture.ARM32 -> ARM32DataLayout
            Architecture.X86 -> X86DataLayout
        }
    }
}

/**
 * x86-64 (AMD64) data layout.
 * Based on System V AMD64 ABI and Windows x64 ABI.
 */
internal object X64DataLayout : TargetDataLayout(
        pointerSize = 8,
        pointerAlignment = 8
) {
    override val int8Alignment = 1
    override val int16Alignment = 2
    override val int32Alignment = 4
    override val int64Alignment = 8
    override val floatAlignment = 4
    override val doubleAlignment = 8
    override val vector128Alignment = 8 // Note: over-aligned objects not supported, using 8
}

/**
 * ARM64 (AArch64) data layout.
 * Based on AAPCS64 and Apple ARM64 ABI.
 */
internal object ARM64DataLayout : TargetDataLayout(
        pointerSize = 8,
        pointerAlignment = 8
) {
    override val int8Alignment = 1
    override val int16Alignment = 2
    override val int32Alignment = 4
    override val int64Alignment = 8
    override val floatAlignment = 4
    override val doubleAlignment = 8
    override val vector128Alignment = 8 // Note: over-aligned objects not supported, using 8
}

/**
 * ARM32 data layout.
 * Based on AAPCS (ARM Architecture Procedure Call Standard).
 */
internal object ARM32DataLayout : TargetDataLayout(
        pointerSize = 4,
        pointerAlignment = 4
) {
    override val int8Alignment = 1
    override val int16Alignment = 2
    override val int32Alignment = 4
    override val int64Alignment = 8
    override val floatAlignment = 4
    override val doubleAlignment = 8
    override val vector128Alignment = 8 // Note: over-aligned objects not supported, using 8
}

/**
 * x86 (i386) data layout.
 * Based on System V i386 ABI.
 */
internal object X86DataLayout : TargetDataLayout(
        pointerSize = 4,
        pointerAlignment = 4
) {
    override val int8Alignment = 1
    override val int16Alignment = 2
    override val int32Alignment = 4
    override val int64Alignment = 4 // Note: i386 uses 4-byte alignment for 64-bit integers
    override val floatAlignment = 4
    override val doubleAlignment = 4 // Note: i386 uses 4-byte alignment for doubles
    override val vector128Alignment = 8 // Note: over-aligned objects not supported, using 8
}

/**
 * Computes struct layout following standard C struct layout rules.
 * This can be used to compute field offsets and struct sizes without calling LLVM.
 */
internal class StructLayoutCalculator(private val dataLayout: TargetDataLayout) {

    data class FieldLayout(val offset: Int, val size: Int, val alignment: Int)

    data class StructLayout(
            val size: Int,
            val alignment: Int,
            val fieldOffsets: List<Int>
    )

    /**
     * Compute the layout of a struct given its field sizes and alignments.
     *
     * @param fields List of pairs (size, alignment) for each field
     * @param packed If true, no padding is inserted between fields
     * @return The computed struct layout
     */
    fun computeLayout(fields: List<Pair<Int, Int>>, packed: Boolean = false): StructLayout {
        if (fields.isEmpty()) {
            return StructLayout(size = 0, alignment = 1, fieldOffsets = emptyList())
        }

        var currentOffset = 0
        var structAlignment = 1
        val fieldOffsets = mutableListOf<Int>()

        for ((fieldSize, fieldAlignment) in fields) {
            if (!packed) {
                // Align the current offset to the field's alignment
                val remainder = currentOffset % fieldAlignment
                if (remainder != 0) {
                    currentOffset += fieldAlignment - remainder
                }
            }

            fieldOffsets.add(currentOffset)
            currentOffset += fieldSize

            if (fieldAlignment > structAlignment) {
                structAlignment = fieldAlignment
            }
        }

        // Pad the struct size to a multiple of the struct alignment
        if (!packed) {
            val remainder = currentOffset % structAlignment
            if (remainder != 0) {
                currentOffset += structAlignment - remainder
            }
        }

        return StructLayout(
                size = currentOffset,
                alignment = structAlignment,
                fieldOffsets = fieldOffsets
        )
    }

    /**
     * Align a value up to the given alignment.
     */
    fun alignTo(value: Int, alignment: Int): Int {
        val remainder = value % alignment
        return if (remainder == 0) value else value + (alignment - remainder)
    }
}

/**
 * Pre-computed layouts for runtime structures.
 * These correspond to C++ structs defined in the runtime (e.g., Memory.h).
 */
internal object RuntimeStructLayouts {

    /**
     * Get the slot count for FrameOverlay.
     * FrameOverlay contains: FrameOverlay* previous + int32_t parameters
     * Slot count = FrameOverlay size / pointer size
     */
    fun getFrameOverlaySlotCount(layout: TargetDataLayout): Int {
        val calculator = StructLayoutCalculator(layout)
        val fields = listOf(
                layout.pointerSize to layout.pointerAlignment, // previous
                layout.int32Size to layout.int32Alignment       // parameters
        )
        val structLayout = calculator.computeLayout(fields)
        return structLayout.size / layout.pointerSize
    }
}
