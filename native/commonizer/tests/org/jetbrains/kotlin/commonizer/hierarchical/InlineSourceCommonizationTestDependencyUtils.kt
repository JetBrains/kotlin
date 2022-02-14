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
    }
}

internal fun InlineSourceBuilder.ModuleBuilder.unsignedIntegers() {
    source(
        """
        package kotlin
        class UByte
        class UShort
        class UInt
        class ULong
        """.trimIndent(), "unsigned.kt"
    )
}

private fun InlineSourceBuilder.ModuleBuilder.unsingedVarIntegers() {
    source(
        """
        package kotlinx.cinterop
        class UByteVarOf
        class UShortVarOf
        class UIntVarOf
        class ULongVarOf
        """.trimIndent(), "UnsignedVarOf.kt"
    )
}

private fun InlineSourceBuilder.ModuleBuilder.singedVarIntegers() {
    source(
        """
        package kotlinx.cinterop
        class ByteVarOf
        class ShortVarOf
        class IntVarOf
        class LongVarOf
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
