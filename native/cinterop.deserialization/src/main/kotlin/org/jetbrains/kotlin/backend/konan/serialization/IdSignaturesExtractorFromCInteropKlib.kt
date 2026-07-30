/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import kotlinx.metadata.klib.KlibMetadataVersion
import kotlinx.metadata.klib.KlibModuleFragmentReadStrategy
import kotlinx.metadata.klib.KlibModuleMetadata
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
import org.jetbrains.kotlin.ir.util.IrErrorModuleFragment
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
import org.jetbrains.kotlin.utils.mapToSetOrEmpty
import kotlin.metadata.ClassName
import kotlin.metadata.KmAnnotation
import kotlin.metadata.KmAnnotationArgument
import kotlin.metadata.KmClass
import kotlin.metadata.KmClassifier
import kotlin.metadata.KmType
import kotlin.metadata.isLocalClassName

/**
 * This is a lightweight tool that allows extracting [IdSignature]s from the given C-interop [KotlinLibrary].
 */
class IdSignaturesExtractorFromCInteropKlib(private val library: KotlinLibrary) : IdSignaturesExtractor {
    init {
        check(library.isCInteropLibrary()) { "Not a C-interop library: ${library.path}" }
    }

    override fun extractAllPublicSignatures(): ExtractedSignatures {
        val [allReferencedClasses: Set<ClassId>, metadataModule] = readMetadataModule(
            loadOnlyTopLevelReferencedClassIds = false
        )

        // All declared classes, including nested classes/companion objects, etc.
        val allDeclaredClasses: Map<ClassId, KmClass> = metadataModule.fragments.asSequence()
            .flatMap { it.classes }
            .filterNot { it.name.isLocalClassName() }
            .associateBy { ClassId.fromString(it.name) }

        val transformer = createTransformer { classId -> allDeclaredClasses[classId] }

        // We have to deserialize each top-level class with all its members/nested classes to get the full set of signatures:
        for ([classId, clazz] in allDeclaredClasses) {
            if (!classId.isNestedClass) {
                transformer.transformTopLevelClass(clazz)
            }
        }

        for (packageFragment in metadataModule.fragments) {
            val pkg = packageFragment.pkg ?: continue
            pkg.functions.forEach(transformer::transformTopLevelFunction)
            pkg.properties.forEach(transformer::transformTopLevelProperty)
        }

        // Get all signatures of declarations contained in the library.
        val allDeclaredSignatures: Map<IdSignature, IrDeclaration> = transformer.declarationTracker.deserializedDeclarations

        // Compute the set of signatures that are "imported".
        val importedSignatures = (allReferencedClasses - allDeclaredClasses.keys).mapToSetOrEmpty { it.computeSignature() }

        // Exclude any private declarations, such as private constructors which happen in C-interop KLIBs.
        val onlyPublicDeclaredSignatures = allDeclaredSignatures
            .mapNotNullTo(hashSetOf()) { [signature, deserializedDeclaration] ->
                signature.takeIf {
                    deserializedDeclaration !is IrDeclarationWithVisibility || deserializedDeclaration.isNonPrivate
                }
            }

        return ExtractedSignatures(
            declaredSignatures = onlyPublicDeclaredSignatures,
            importedSignatures = importedSignatures,
        )
    }

    override fun extractOnlyTopLevelPublicSignatures(): ExtractedSignatures {
        val [onlyTopLevelReferencedClasses, metadataModule] = readMetadataModule(
            loadOnlyTopLevelReferencedClassIds = true
        )

        val transformer = createTransformer { null }

        val declaredTopLevelClasses = hashSetOf<ClassId>()

        for (packageFragment in metadataModule.fragments) {
            // Just extract the signature of the top-level class
            for (clazz in packageFragment.classes) {
                if (clazz.name.isLocalClassName()) continue

                val classId = ClassId.fromString(clazz.name)
                if (classId.isNestedClass) continue

                declaredTopLevelClasses += classId
            }

            val pkg = packageFragment.pkg ?: continue
            pkg.functions.forEach(transformer::transformTopLevelFunction)
            pkg.properties.forEach(transformer::transformTopLevelProperty)
        }

        val importedSignatures = (onlyTopLevelReferencedClasses - declaredTopLevelClasses).mapToSetOrEmpty { it.computeSignature() }

        return ExtractedSignatures(
            declaredSignatures = transformer.declarationTracker.deserializedDeclarations.keys + declaredTopLevelClasses.mapToSetOrEmpty { it.computeSignature() },
            importedSignatures = importedSignatures,
        )
    }

    private fun readMetadataModule(loadOnlyTopLevelReferencedClassIds: Boolean): Pair<Set<ClassId>, KlibModuleMetadata> {
        val strategy = KlibModuleFragmentReadStrategyImpl(loadOnlyTopLevelReferencedClassIds)

        val metadataModule = KlibModuleMetadata.readLenient(
            library = MetadataLibraryProviderImpl(library = library),
            readStrategy = strategy
        )

        return strategy.referencedClassIds to metadataModule
    }

    private fun createTransformer(getNestedKmClass: (ClassId) -> KmClass?): CInteropKlibMetadata2IRTransformer {
        val packageFragment = IrExternalPackageFragmentImpl(
            symbol = IrExternalPackageFragmentSymbolImpl(),
            packageFqName = library.packageFqName?.let(::FqName) ?: error("C-interop library without the package name: ${library.path}"),
            module = IrErrorModuleFragment,
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

    private fun ClassId.computeSignature(): IdSignature {
        // Guess, whether it comes from a standard library or another C-interop library,
        // and create the appropriate signature.
        return toCInteropSignature(isCInterop = !definitelyNotFromCInterop())
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

    private class KlibModuleFragmentReadStrategyImpl(
        private val loadOnlyTopLevelReferencedClassIds: Boolean
    ) : KlibModuleFragmentReadStrategy {

        val referencedClassIds: Set<ClassId>
            field = hashSetOf()

        override fun processType(type: KmType) {
            processReferencedClassName((type.classifier as? KmClassifier.Class)?.name ?: return)
        }

        override fun processAnnotation(annotation: KmAnnotation) {
            processReferencedClassName(annotation.className)
            annotation.arguments.values.forEach(::processAnnotationArgument)
        }

        private fun processAnnotationArgument(argument: KmAnnotationArgument) {
            when (argument) {
                is KmAnnotationArgument.LiteralValue<*> -> /* nothing to do */ Unit
                is KmAnnotationArgument.AnnotationValue -> processAnnotation(argument.annotation)
                is KmAnnotationArgument.ArrayValue -> argument.elements.forEach(::processAnnotationArgument)
                is KmAnnotationArgument.KClassValue -> processReferencedClassName(argument.className)
                is KmAnnotationArgument.ArrayKClassValue -> processReferencedClassName(argument.className)
                is KmAnnotationArgument.EnumValue -> processReferencedClassName(
                    if (loadOnlyTopLevelReferencedClassIds)
                        argument.enumClassName
                    else
                        argument.enumClassName + "." + argument.enumEntryName
                )
            }
        }

        private fun processReferencedClassName(className: ClassName) {
            if (className.isLocalClassName()) return

            // Extract class ID from a String.
            val classId = ClassId.fromString(className)

            if (!loadOnlyTopLevelReferencedClassIds || !classId.isNestedClass) {
                referencedClassIds += classId
            }
        }
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
