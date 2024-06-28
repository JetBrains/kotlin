/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.IrLibraryFileFromBytes.Companion.extensionRegistryLite
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration.DeclaratorCase.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.impl.IrArrayMemoryReader
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.utils.addToStdlib.ifTrue
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFile as ProtoFile
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclaration as ProtoDeclaration
import org.jetbrains.kotlin.backend.common.serialization.proto.IrDeclarationBase as ProtoDeclarationBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrClass as ProtoClass
import org.jetbrains.kotlin.backend.common.serialization.proto.IrTypeAlias as ProtoTypeAlias
import org.jetbrains.kotlin.backend.common.serialization.proto.IrFunctionBase as ProtoFunctionBase
import org.jetbrains.kotlin.backend.common.serialization.proto.IrProperty as ProtoProperty

internal class IrSignaturesExtractor(private val library: KotlinLibrary) {
    data class Signatures(
            val declaredSignatures: Set<IdSignature>,
            val importedSignatures: Set<IdSignature>
    )

    private val interner = IrInterningService()

    private inner class IrSignatureExtractorFromFile(
            private val fileIndex: Int,
            private val allKnownSignatures: MutableSet<IdSignature>,
            private val ownDeclarationSignatures: OwnDeclarationSignatures
    ) {
        private val fileProto = ProtoFile.parseFrom(library.file(fileIndex).codedInputStream, extensionRegistryLite)
        private val fileReader = IrLibraryFileFromBytes(IrKlibBytesSource(library, fileIndex))

        private val signatureDeserializer: IdSignatureDeserializer = run {
            val packageFQN = fileReader.deserializeFqName(fileProto.fqNameList)
            val fileName = if (fileProto.hasFileEntry() && fileProto.fileEntry.hasName()) fileProto.fileEntry.name else "<unknown>"

            val fileSignature = IdSignature.FileSignature(
                    id = Any(), // Just an unique object.
                    fqName = FqName(packageFQN),
                    fileName = fileName
            )
            IdSignatureDeserializer(fileReader, fileSignature, interner)
        }

        fun extract() {
            collectAllKnownSignatures()
            collectSignaturesFromDeclarations()
        }

        private fun collectAllKnownSignatures() {
            val maxSignatureIndex = IrArrayMemoryReader(library.signatures(fileIndex)).entryCount() - 1 // Index of the latest signature in the current file.
            (0..maxSignatureIndex).mapTo(allKnownSignatures, signatureDeserializer::deserializeIdSignature)
        }

        private fun collectSignaturesFromDeclarations() {
            for (topLevelDeclarationIndex in fileProto.declarationIdList) {
                extractSignature(declarationProto = fileReader.declaration(topLevelDeclarationIndex), isParentPrivate = false)
            }
        }

        private fun extractSignature(declarationProto: ProtoDeclaration, isParentPrivate: Boolean) {
            when (declarationProto.declaratorCase) {
                IR_CLASS -> extractSignatureFromClass(declarationProto.irClass, isParentPrivate)
                IR_CONSTRUCTOR -> extractSignatureFromFunction(declarationProto.irConstructor.base, isParentPrivate)
                IR_FUNCTION -> extractSignatureFromFunction(declarationProto.irFunction.base, isParentPrivate)
                IR_PROPERTY -> extractSignatureFromProperty(declarationProto.irProperty, isParentPrivate)
                IR_TYPE_ALIAS -> extractSignatureFromTypeAlias(declarationProto.irTypeAlias, isParentPrivate)
                IR_ENUM_ENTRY -> extractSignatureFromSymbol(declarationProto.irEnumEntry.base.symbol, isPrivate = isParentPrivate)
                IR_ANONYMOUS_INIT -> extractSignatureFromPrivateDeclaration(declarationProto.irAnonymousInit.base)
                IR_TYPE_PARAMETER -> extractSignatureFromPrivateDeclaration(declarationProto.irTypeParameter.base)
                IR_FIELD -> extractSignatureFromPrivateDeclaration(declarationProto.irField.base)
                IR_VARIABLE -> extractSignatureFromPrivateDeclaration(declarationProto.irVariable.base)
                IR_VALUE_PARAMETER -> extractSignatureFromPrivateDeclaration(declarationProto.irValueParameter.base)
                IR_LOCAL_DELEGATED_PROPERTY -> extractSignatureFromPrivateDeclaration(declarationProto.irLocalDelegatedProperty.base)
                IR_ERROR_DECLARATION, DECLARATOR_NOT_SET, null -> Unit
            }
        }

        private fun extractSignatureFromClass(classProto: ProtoClass, isParentPrivate: Boolean) {
            val isPrivate = isParentPrivate || classProto.base.isPrivate()
            extractSignatureFromSymbol(classProto.base.symbol, isPrivate)
            classProto.declarationList.forEach { extractSignature(it, isPrivate) }
        }

        private fun extractSignatureFromFunction(functionProto: ProtoFunctionBase, isParentPrivate: Boolean) {
            val isPrivate = isParentPrivate || functionProto.base.isPrivate()
            extractSignatureFromSymbol(functionProto.base.symbol, isPrivate)
        }

        private fun extractSignatureFromProperty(propertyProto: ProtoProperty, isParentPrivate: Boolean) {
            val isPrivate = isParentPrivate || propertyProto.base.isPrivate()
            extractSignatureFromSymbol(propertyProto.base.symbol, isPrivate)
            propertyProto.hasGetter().ifTrue { extractSignatureFromFunction(propertyProto.getter.base, isPrivate) }
            propertyProto.hasSetter().ifTrue { extractSignatureFromFunction(propertyProto.setter.base, isPrivate) }
            propertyProto.hasBackingField().ifTrue { extractSignatureFromPrivateDeclaration(propertyProto.backingField.base) }
        }

        private fun extractSignatureFromTypeAlias(typeAliasProto: ProtoTypeAlias, isParentPrivate: Boolean) {
            val isPrivate = isParentPrivate || typeAliasProto.base.isPrivate()
            extractSignatureFromSymbol(typeAliasProto.base.symbol, isPrivate)
        }

        private fun extractSignatureFromPrivateDeclaration(declarationProto: ProtoDeclarationBase) {
            extractSignatureFromSymbol(declarationProto.symbol, isPrivate = true)
        }

        private fun extractSignatureFromSymbol(symbolId: Long, isPrivate: Boolean) {
            val signatureId = BinarySymbolData.decode(symbolId).signatureId
            val signature = signatureDeserializer.deserializeIdSignature(signatureId)
            ownDeclarationSignatures[signature] = !isPrivate
        }

        private fun ProtoDeclarationBase.isPrivate(): Boolean =
                when (IrFlags.VISIBILITY.get(flags.toInt())) {
                    ProtoBuf.Visibility.PUBLIC,
                    ProtoBuf.Visibility.PROTECTED,
                    ProtoBuf.Visibility.INTERNAL -> false
                    ProtoBuf.Visibility.PRIVATE,
                    ProtoBuf.Visibility.PRIVATE_TO_THIS,
                    ProtoBuf.Visibility.LOCAL,
                    null -> true
                }
    }

    fun extract(): Signatures {
        val allKnownSignatures: MutableSet<IdSignature> = hashSetOf()
        val ownDeclarationSignatures: OwnDeclarationSignatures = hashMapOf()

        for (fileIndex in 0 until library.fileCount()) {
            IrSignatureExtractorFromFile(fileIndex, allKnownSignatures, ownDeclarationSignatures).extract()
        }

        val outermostOwnDeclarationSignatures: MutableSet<IdSignature> = hashSetOf()
        ownDeclarationSignatures.keys.forEach { signature ->
            outermostOwnDeclarationSignatures += signature.getOutermostSignature()
            if (signature is IdSignature.CompositeSignature && signature.container is IdSignature.FileSignature)
                outermostOwnDeclarationSignatures += signature.inner
        }

        val importedSignatures = allKnownSignatures.filterTo(hashSetOf()) { signature ->
            if (signature.isLocal || !signature.isPubliclyVisible)
                false
            else {
                val outermostSignature = signature.getOutermostSignature()
                outermostSignature !is IdSignature.FileSignature && outermostSignature !in outermostOwnDeclarationSignatures
            }
        }

        return Signatures(
                declaredSignatures = ownDeclarationSignatures.entries.mapNotNullTo(hashSetOf()) { (signature, isPublic) -> signature.takeIf { isPublic } },
                importedSignatures = importedSignatures
        )
    }

    private fun IdSignature.getOutermostSignature() = if (hasTopLevel) topLevelSignature() else this
}

private typealias OwnDeclarationSignatures = MutableMap<IdSignature, /* isPublic? */ Boolean>
