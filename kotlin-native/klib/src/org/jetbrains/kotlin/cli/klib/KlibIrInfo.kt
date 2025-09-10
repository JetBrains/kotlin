/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.DeclarationIdTableReader

/**
 * Some information obtained from library's IR.
 *
 * @property preparedInlineFunctionCopyNumber The number of inline functions that are stored aside in klib and can be inlined
 * on the first stage of compilation.
 */
internal class KlibIrInfo(
    val preparedInlineFunctionCopyNumber: Int
)

internal class KlibIrInfoLoader(private val library: KotlinLibrary) {
    fun loadIrInfo(): KlibIrInfo? {
        if (!library.hasIrOfInlineableFuns) return null

        val declarationsReader = DeclarationIdTableReader(library.declarationsOfInlineableFuns())
        val preparedInlineFunctionCopyNumber = declarationsReader.entryCount()

        return KlibIrInfo(
            preparedInlineFunctionCopyNumber = preparedInlineFunctionCopyNumber,
        )
    }
}
