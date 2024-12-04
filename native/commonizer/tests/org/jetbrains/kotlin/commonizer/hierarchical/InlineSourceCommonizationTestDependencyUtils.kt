/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.hierarchical

import org.jetbrains.kotlin.commonizer.AbstractInlineSourcesCommonizationTest
import org.jetbrains.kotlin.commonizer.parseCommonizerTarget
import org.jetbrains.kotlin.commonizer.utils.InlineSourceBuilder
import org.jetbrains.kotlin.commonizer.withAllLeaves

internal fun AbstractInlineSourcesCommonizationTest.ParametersBuilder.registerFakeStdlibIntegersDependency(vararg outputTarget: String) {
    val allTargets = outputTarget.map { parseCommonizerTarget(it) }.withAllLeaves()
    registerDependency(*allTargets.toTypedArray()) {
        unsignedIntegers()
        unsingedVarIntegers()
        singedVarIntegers()
        unsafeNumberAnnotationSource()
        ranges()
        platformIntegers()
    }
}

internal fun InlineSourceBuilder.ModuleBuilder.unsignedIntegers() {
    source(
        """
        package kotlin
        
        class UByte : Comparable<UByte> { override fun compareTo(other: UByte): Int = null!! }
        class UShort : Comparable<UShort> { override fun compareTo(other: UShort): Int = null!! }
        class UInt : Comparable<UInt> { override fun compareTo(other: UInt): Int = null!! }
        class ULong : Comparable<ULong> { override fun compareTo(other: ULong): Int = null!! }
        
        class UByteArray
        class UShortArray
        class UIntArray
        class ULongArray
        """.trimIndent(), "unsigned.kt"
    )
}

private fun InlineSourceBuilder.ModuleBuilder.unsingedVarIntegers() {
    source(
        """
        package kotlinx.cinterop
        class UByteVarOf<T : UByte>
        class UShortVarOf<T : UShort>
        class UIntVarOf<T : UInt>
        class ULongVarOf<T : ULong>
        """.trimIndent(), "UnsignedVarOf.kt"
    )
}

private fun InlineSourceBuilder.ModuleBuilder.singedVarIntegers() {
    source(
        """
        package kotlinx.cinterop
        class ByteVarOf<T : Byte>
        class ShortVarOf<T : Short>
        class IntVarOf<T : Int>
        class LongVarOf<T : Long>
        """.trimIndent(), "SignedVarOf.kt"
    )
}

internal fun InlineSourceBuilder.ModuleBuilder.unsafeNumberAnnotationSource() {
    source(
        """
            package kotlinx.cinterop
            @Target(AnnotationTarget.TYPEALIAS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
            @Retention(AnnotationRetention.BINARY)
            annotation class UnsafeNumber(val actualPlatformTypes: Array<String>)
        """.trimIndent(),
        "UnsafeNumberAnnotation.kt"
    )

    source(
        """
            typealias UnsafeNumber = kotlinx.cinterop.UnsafeNumber
        """.trimIndent(),
        "UnsafeNumberTypeAlias.kt"
    )
}

internal fun InlineSourceBuilder.ModuleBuilder.ranges() {
    source(
        """
            package kotlin.ranges
            
            interface ClosedRange<T : Comparable<T>>
            
            open class IntProgression
            open class UIntProgression
            open class LongProgression
            open class ULongProgression
            
            class IntRange : IntProgression(), ClosedRange<Int>
            class UIntRange : UIntProgression(), ClosedRange<UInt>
            class LongRange : LongProgression(), ClosedRange<Long>
            class ULongRange : ULongProgression(), ClosedRange<ULong>
        """.trimIndent(),
        "Ranges.kt"
    )
}

private fun InlineSourceBuilder.ModuleBuilder.platformIntegers() {
    source(
        """
            package kotlin
            
            expect class PlatformInt : Number, Comparable<PlatformInt>
            expect class PlatformUInt : Comparable<PlatformUInt>
            expect class PlatformIntArray
            expect class PlatformUIntArray
        """.trimIndent(),
        "PlatformInts.kt"
    )

    source(
        """
            package kotlinx.cinterop
            
            abstract class CVariable
            
            expect class PlatformIntVarOf<T : PlatformInt> : CVariable
            expect class PlatformUIntVarOf<T : PlatformUInt> : CVariable
        """.trimIndent(),
        "PlatformVars.kt"
    )

    source(
        """
            package kotlin.ranges
            
            expect open class PlatformIntProgression
            expect open class PlatformUIntProgression
            expect class PlatformIntRange : PlatformIntProgression, ClosedRange<PlatformInt>
            expect class PlatformUIntRange : PlatformUIntProgression, ClosedRange<PlatformUInt>
        """.trimIndent()
    )
}
