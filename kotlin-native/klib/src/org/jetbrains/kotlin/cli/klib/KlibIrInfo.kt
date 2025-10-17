/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.protobuf.ExtensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile

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
        if (!library.hasInlinableFunsIr) return null

        val fileStream = library.inlinableFunsIr.file(0).codedInputStream
        val fileProto = ProtoFile.parseFrom(fileStream, ExtensionRegistryLite.newInstance())

        return KlibIrInfo(
            preparedInlineFunctionCopyNumber = fileProto.declarationIdList.size,
        )
    }
}
