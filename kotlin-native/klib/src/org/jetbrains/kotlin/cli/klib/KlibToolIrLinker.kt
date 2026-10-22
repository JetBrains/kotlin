/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.linkage.partial.PartiallyLinkedDeclarationOrigin
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.BasicIrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.DeserializationStrategy
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.common.serialization.deserializeIrSymbolOrFail
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData.SymbolKind
import org.jetbrains.kotlin.backend.konan.serialization.KonanInteropModuleDeserializer
import org.jetbrains.kotlin.backend.konan.serialization.KonanManglerIr
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.createEmptyExternalPackageFragment
import org.jetbrains.kotlin.ir.expressions.IrLazilyBoundAnnotationImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrConstructorSymbolImpl
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.resolve.CommonCompilerDeserializationConfiguration

internal class KlibToolIrLinker(
        output: KlibToolOutput,
        symbolTable: SymbolTable,
) : KotlinIrLinker(symbolTable, errorCallback = output::logError) {
    override val irMangler: KotlinMangler.IrMangler = KonanManglerIr

    override val fakeOverrideBuilder = IrLinkerFakeOverrideProvider(
            linker = this,
            symbolTable = symbolTable,
            mangler = irMangler,
            friendModules = emptyMap(), // TODO(KT-62534) can be removed when ModuleDescriptorImpl.shouldSeeInternalsOf is fixed
            partialLinkageSupport = PartialLinkageSupportForLinker.DISABLED,
    )

    override val returnUnboundSymbolsIfSignatureNotFound get() = true

    override fun createModuleDeserializer(
            moduleFragment: IrModuleFragment,
            klib: KotlinLibrary?,
            strategyResolver: (String) -> DeserializationStrategy,
    ): IrModuleDeserializer = when {
        klib == null -> {
            if (moduleFragment.name == EMULATED_STDLIB_MODULE_NAME)
                EmulatedStandardLibraryModuleDeserializer(moduleFragment)
            else
                error("Expecting kotlin library for $moduleFragment")
        }

        klib.isCInteropLibrary() -> KonanInteropModuleDeserializer(
                deserializationConfiguration = CommonCompilerDeserializationConfiguration(LanguageVersionSettingsImpl.DEFAULT),
                moduleFragment = moduleFragment,
                klib = klib,
                isLibraryCached = false,
                linker = this,
                strategyResolver = strategyResolver,
        )

        else -> KlibToolModuleDeserializer(
                moduleFragment = moduleFragment,
                klib = klib,
                strategyResolver = strategyResolver
        )
    }

    override fun isBuiltInModule(module: IrModuleFragment) = false

    private inner class KlibToolModuleDeserializer(
            moduleFragment: IrModuleFragment,
            klib: KotlinLibrary,
            strategyResolver: (String) -> DeserializationStrategy,
    ) : BasicIrModuleDeserializer(
            linker = this,
            moduleFragment = moduleFragment,
            strategyResolver = strategyResolver,
            klib = klib,
            libraryAbiVersion = klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT,
            allowErrorNodes = true,
            deserializeTypeAliases = true,
    )

    /**
     * An artificial module deserializer that emulates the part of the Kotlin standard library which is referenced
     * from annotations in C-interop libraries: the annotation classes from `kotlin` and `kotlinx.cinterop` package.
     *
     * Why it is needed: [KonanInteropModuleDeserializer] creates annotations as [IrLazilyBoundAnnotationImpl], which know only
     * the annotation class symbol and the arguments by name. On the first access to the constructor symbol or to the arguments
     * (for example, when rendering IR in `dump-ir`), such an annotation asks the linker for the annotation class to find its primary
     * constructor. The KLIB tool does not load the standard library, so the linker can't find the class and the annotation fails
     * with an NPE. This deserializer creates a stub annotation class instead.
     *
     * [getDefinedPackageNames] returns `null`, so the linker consults this deserializer only after all regular ones. If a real
     * module defines the same declarations (e.g., when dumping the standard library itself), the real module wins.
     */
    private inner class EmulatedStandardLibraryModuleDeserializer(
            moduleFragment: IrModuleFragment,
    ) : IrModuleDeserializer(moduleFragment, KotlinAbiVersion.CURRENT) {
        private val syntheticPackageFragment by lazy {
            createEmptyExternalPackageFragment(IrErrorModuleFragment, FqName.ROOT)
        }

        // We cannot load the real nothing class. So let's use an emulated one here.
        private val emulatedNothingClass: IrClassSymbol by lazy {
            deserializeIrSymbolOrFail(StandardClassIds.Nothing.toIdSignature(), CLASS_SYMBOL) as IrClassSymbol
        }

        override val klib: KotlinLibrary get() = error("The emulated standard library has no Klib")
        override fun getDefinedPackageNames(): Set<FqName>? = null

        override fun contains(idSig: IdSignature): Boolean {
            val packageFqName = (idSig as? IdSignature.CommonSignature)?.packageFqName() ?: return false
            return EMULATED_PACKAGE_NAMES.any { packageFqName.startsWith(it) }
        }

        override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: SymbolKind): IrSymbol? {
            if (symbolKind != CLASS_SYMBOL || idSig !in this) return null

            return symbolTable.declareClassIfNotExists(
                    signature = idSig,
                    symbolFactory = { IrClassSymbolImpl(signature = idSig) },
                    classFactory = ::generateStubClass,
            ).symbol
        }

        override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing = error("No emulated standard library symbol found for $idSig")
        override fun deserializeReachableDeclarations() = Unit

        @OptIn(UnsafeDuringIrConstructionAPI::class)
        private fun generateStubClass(symbol: IrClassSymbol): IrClass = with(symbolTable.irFactory) {
            return createClass(
                    startOffset = UNDEFINED_OFFSET,
                    endOffset = UNDEFINED_OFFSET,
                    origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
                    name = Name.identifier((symbol.signature as IdSignature.CommonSignature).shortName),
                    visibility = DescriptorVisibilities.DEFAULT_VISIBILITY,
                    symbol = symbol,
                    kind = ClassKind.CLASS,
                    modality = Modality.OPEN,
            ).apply {
                parent = syntheticPackageFragment
                createThisReceiverParameter()
            }.also { clazz ->
                // Generate private default constructor.
                createConstructor(
                        startOffset = UNDEFINED_OFFSET,
                        endOffset = UNDEFINED_OFFSET,
                        origin = PartiallyLinkedDeclarationOrigin.MISSING_DECLARATION,
                        name = SpecialNames.INIT,
                        visibility = DescriptorVisibilities.PRIVATE,
                        isInline = false,
                        isExpect = false,
                        returnType = emulatedNothingClass.defaultType,
                        symbol = IrConstructorSymbolImpl(),
                        isPrimary = true,
                        isExternal = false,
                ).apply {
                    parent = clazz
                    clazz.declarations += this
                }
            }
        }
    }

    companion object {
        private val EMULATED_PACKAGE_NAMES = setOf(FqName("kotlinx.cinterop"), FqName("kotlin"))
    }
}
