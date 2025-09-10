/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.common.serialization.IrKlibBytesSource
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFileFromBytes
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFileFromBytes.Companion.extensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.codedInputStream
import org.jetbrains.kotlin.backend.common.serialization.encodings.FunctionFlags
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.DeclaratorCase.IR_CLASS
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.DeclaratorCase.IR_CONSTRUCTOR
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.DeclaratorCase.IR_FUNCTION
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.DeclaratorCase.IR_PROPERTY
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities.isPrivate
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClass as ProtoClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionBase as ProtoFunctionBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrProperty as ProtoProperty

/**
 * Some information obtained from library's IR.
 *
 * @property meaningfulInlineFunctionNumber The number of meaningful (non-private, non-local, with body, etc.) inline functions.
 * @property meaningfulInlineFunctionBodiesSize The cumulative size in bytes of all meaningful inline function bodies.
 */
internal class KlibIrInfo(
    val meaningfulInlineFunctionNumber: Int,
    val meaningfulInlineFunctionBodiesSize: Long
)

internal class KlibIrInfoLoader(private val library: KotlinLibrary) {
    var meaningfulInlineFunctionNumber = 0
    var estimationOfInlineBodiesSizes = 0L

    private inner class KlibIrInfoLoaderFromFile(private val fileIndex: Int) {
        private val fileProto = ProtoFile.parseFrom(library.file(fileIndex).codedInputStream, extensionRegistryLite)
        private val fileByteSource = IrKlibBytesSource(library, fileIndex)
        private val fileReader = IrLibraryFileFromBytes(fileByteSource)

        fun load() {
            for (topLevelDeclarationIndex in fileProto.declarationIdList) {
                inspectDeclaration(fileReader.declaration(topLevelDeclarationIndex))
            }
        }

        private fun inspectDeclaration(declarationProto: ProtoDeclaration) {
            when (declarationProto.declaratorCase) {
                IR_CLASS -> inspectClass(declarationProto.irClass)
                IR_CONSTRUCTOR -> inspectFunction(declarationProto.irConstructor.base)
                IR_FUNCTION -> inspectFunction(declarationProto.irFunction.base)
                IR_PROPERTY -> inspectProperty(declarationProto.irProperty)
                else -> Unit
            }
        }

        private fun inspectClass(classProto: ProtoClass) {
            classProto.declarationList.forEach { inspectDeclaration(it) }
        }

        private fun inspectFunction(functionProto: ProtoFunctionBase) {
            if (!functionProto.hasBody()) return

            val protoBase = functionProto.base
            if (!protoBase.hasFlags()) return

            val flags = FunctionFlags.decode(protoBase.flags)
            if (!flags.isInline || isPrivate(flags.visibility)) return

            meaningfulInlineFunctionNumber++
            estimationOfInlineBodiesSizes += fileByteSource.body(functionProto.body).size
        }

        private fun inspectProperty(propertyProto: ProtoProperty) {
            propertyProto.hasGetter().ifTrue { inspectFunction(propertyProto.getter.base) }
            propertyProto.hasSetter().ifTrue { inspectFunction(propertyProto.setter.base) }
        }
    }

    fun loadIrInfo(): KlibIrInfo? {
        if (!library.hasIr) return null

        for (fileIndex in 0 until library.fileCount()) {
            KlibIrInfoLoaderFromFile(fileIndex).load()
        }

        return KlibIrInfo(
            meaningfulInlineFunctionNumber = meaningfulInlineFunctionNumber,
            meaningfulInlineFunctionBodiesSize = estimationOfInlineBodiesSizes,
        )
    }
}
