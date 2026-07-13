/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import kotlinx.metadata.klib.KlibMetadataVersion
import kotlinx.metadata.klib.KlibModuleMetadata
import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.backend.common.IdSignaturesExtractor
import org.jetbrains.kotlin.backend.common.IdSignaturesExtractor.ExtractedSignatures
import org.jetbrains.kotlin.backend.common.serialization.referenceDeserializedSymbol
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrProvider
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFactoryImpl
import org.jetbrains.kotlin.ir.overrides.isNonPrivate
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.classIdWhenAvailable
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.components.KlibMetadataComponent
import org.jetbrains.kotlin.library.components.metadata
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.library.metadataVersion
import org.jetbrains.kotlin.library.packageFqName
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import kotlin.metadata.KmClass
import kotlin.metadata.isLocalClassName

/**
 * This is a lightweight tool that allows extracting [IdSignature]s from the given C-interop [KotlinLibrary].
 */
@OptIn(K1Deprecation::class)
class IdSignaturesExtractorFromCInteropKlib(private val library: KotlinLibrary) : IdSignaturesExtractor {
    init {
        check(library.isCInteropLibrary()) { "Not a C-interop library: ${library.path}" }
    }

    override fun extractAllPublicSignatures(): ExtractedSignatures {
        val metadataModule = readMetadataModule()

        // All classes, including nested classes/companion objects, etc.
        val allClasses: Map<ClassId, KmClass> = metadataModule.fragments.asSequence()
            .flatMap { it.classes }
            .filterNot { it.name.isLocalClassName() }
            .associateBy { ClassId.fromString(it.name) }

        val transformer = createTransformer { classId -> allClasses[classId] }

        // We have to deserialize each top-level class with all its members/nested classes to get the full set of signatures:
        for ([classId, clazz] in allClasses) {
            if (!classId.isNestedClass) {
                transformer.transformTopLevelClass(clazz)
            }
        }

        for (packageFragment in metadataModule.fragments) {
            val pkg = packageFragment.pkg ?: continue
            pkg.functions.forEach(transformer::transformTopLevelFunction)
            pkg.properties.forEach(transformer::transformTopLevelProperty)
        }

        // Exclude any private declarations, such as private constructors which happen in C-interop KLIBs.
        val declaredSignatures = transformer.declarationTracker.deserializedDeclarations
            .mapNotNullTo(hashSetOf()) { [signature, deserializedDeclaration] ->
                signature.takeIf {
                    deserializedDeclaration !is IrDeclarationWithVisibility || deserializedDeclaration.isNonPrivate
                }
            }

        return ExtractedSignatures(
            declaredSignatures = declaredSignatures,
            importedSignatures = emptySet(), // TODO(KT-86840): Implement extracting imported signatures as well.
        )
    }

    override fun extractOnlyTopLevelPublicSignatures(): ExtractedSignatures {
        val metadataModule = readMetadataModule()
        val transformer = createTransformer { null }

        val declaredClassSignatures = hashSetOf<IdSignature>()

        for (packageFragment in metadataModule.fragments) {
            // Just extract the signature of the top-level class
            for (clazz in packageFragment.classes) {
                if (clazz.name.isLocalClassName()) continue

                val classId = ClassId.fromString(clazz.name)
                if (classId.isNestedClass) continue

                declaredClassSignatures += classId.toCInteropSignature(isCInterop = true)
            }

            val pkg = packageFragment.pkg ?: continue
            pkg.functions.forEach(transformer::transformTopLevelFunction)
            pkg.properties.forEach(transformer::transformTopLevelProperty)
        }

        return ExtractedSignatures(
            declaredSignatures = transformer.declarationTracker.deserializedDeclarations.keys + declaredClassSignatures,
            importedSignatures = emptySet(), // TODO(KT-86840): Implement extracting imported signatures as well.
        )
    }

    private fun readMetadataModule() = KlibModuleMetadata.readLenient(MetadataLibraryProviderImpl(library))

    private fun createTransformer(getNestedKmClass: (ClassId) -> KmClass?): CInteropKlibMetadata2IRTransformer {
        val packageFragment = IrExternalPackageFragmentImpl(
            symbol = IrExternalPackageFragmentSymbolImpl(),
            packageFqName = library.packageFqName?.let(::FqName) ?: error("C-interop library without the package name: ${library.path}"),
        )

        val symbolTable = SymbolTable(signaturer = null, IrFactoryImpl)

        return CInteropKlibMetadata2IRTransformer(
            symbolTable = symbolTable,
            symbols = CInteropKlibMetadata2IRTransformer.ExternalSymbols(symbolTable),
            declarationTracker = CInteropKlibMetadata2IRTransformer.DeclarationTracker(),
            getNestedKmClass = getNestedKmClass,
            getOrCreateContainingPackageFragment = { packageFragment },
            getReferencedDeclarationSymbol = { signature, kind ->
                referenceDeserializedSymbol(symbolTable, fileSymbol = null, kind, signature)
            },
            irProviderForLazyAnnotations = StubAnnotationGenerator,
        )
    }

    private class MetadataLibraryProviderImpl(library: KotlinLibrary) : KlibModuleMetadata.MetadataLibraryProvider {
        private val metadata: KlibMetadataComponent = library.metadata

        override val metadataVersion = KlibMetadataVersion(
            library.metadataVersion?.toArray() ?: error("No metadata version specified in ${library.path}")
        )

        override val moduleHeaderData get() = metadata.moduleHeaderData
        override fun packageMetadataParts(fqName: String) = metadata.getPackageFragmentNames(fqName)
        override fun packageMetadata(fqName: String, partName: String) = metadata.getPackageFragment(fqName, partName)
    }

    private object StubAnnotationGenerator : IrProvider {
        override fun getDeclaration(symbol: IrSymbol): IrDeclaration {
            if (symbol.isBound) return symbol.owner as IrDeclaration

            return IrFactoryImpl.createClass(
                startOffset = UNDEFINED_OFFSET,
                endOffset = UNDEFINED_OFFSET,
                origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB,
                name = (symbol as IrClassSymbol).classIdWhenAvailable!!.shortClassName,
                visibility = DescriptorVisibilities.PUBLIC,
                symbol = symbol,
                kind = ClassKind.ANNOTATION_CLASS,
                modality = Modality.FINAL,
            )
        }
    }
}
