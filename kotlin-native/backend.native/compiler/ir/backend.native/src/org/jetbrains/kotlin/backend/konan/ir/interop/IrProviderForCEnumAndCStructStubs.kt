/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin.backend.konan.ir.interop

import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.konan.descriptors.getPackageFragments
import org.jetbrains.kotlin.backend.konan.ir.KonanSymbols
import org.jetbrains.kotlin.backend.konan.ir.interop.cenum.CEnumByValueFunctionGenerator
import org.jetbrains.kotlin.backend.konan.ir.interop.cenum.CEnumClassGenerator
import org.jetbrains.kotlin.backend.konan.ir.interop.cenum.CEnumCompanionGenerator
import org.jetbrains.kotlin.backend.konan.ir.interop.cenum.CEnumVarClassGenerator
import org.jetbrains.kotlin.backend.konan.ir.interop.cstruct.CStructVarClassGenerator
import org.jetbrains.kotlin.backend.konan.ir.interop.cstruct.CStructVarCompanionGenerator
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.psi2ir.generators.GeneratorContext
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter

/**
 * For the most of descriptors that come from metadata-based interop libraries
 * we generate a lazy IR.
 * We use a different approach for CEnums and CStructVars and generate IR eagerly. Motivation:
 * 1. CEnums are "real" Kotlin enums. Thus, we need apply the same compilation approach
 *   as we use for usual Kotlin enums.
 *   Eager generation allows to reuse [EnumClassLowering], [EnumConstructorsLowering] and other
 *   compiler phases.
 * 2. It is an easier and more obvious approach. Since implementation of metadata-based
 *  libraries generation already took too much time we take an easier approach here.
 */
@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class IrProviderForCEnumAndCStructStubs(
        context: GeneratorContext,
        symbols: KonanSymbols
) {

    /**
     *  TODO: integrate this provider into [KonanIrLinker.KonanInteropModuleDeserializer]
     */
    private val symbolTable: SymbolTable = context.symbolTable

    private val cEnumByValueFunctionGenerator =
            CEnumByValueFunctionGenerator(context, symbols)
    private val cEnumCompanionGenerator =
            CEnumCompanionGenerator(context, cEnumByValueFunctionGenerator)
    private val cEnumVarClassGenerator =
            CEnumVarClassGenerator(context, symbols)
    private val cEnumClassGenerator =
            CEnumClassGenerator(context, cEnumCompanionGenerator, cEnumVarClassGenerator)
    private val cStructCompanionGenerator =
            CStructVarCompanionGenerator(context, symbols)
    private val cStructClassGenerator =
            CStructVarClassGenerator(context, cStructCompanionGenerator, symbols)

    fun isCEnumOrCStruct(declarationDescriptor: DeclarationDescriptor): Boolean =
            declarationDescriptor.run { findCEnumDescriptor() ?: findCStructDescriptor() } != null

    fun referenceAllEnumsAndStructsFrom(interopModule: ModuleDescriptor) = interopModule.getPackageFragments()
            .flatMap { it.getMemberScope().getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS) }
            .filterIsInstance<ClassDescriptor>()
            .filter { it.implementsCEnum() || it.inheritsFromCStructVar() }
            .forEach { symbolTable.descriptorExtension.referenceClass(it) }

    private fun generateIrIfNeeded(symbol: IrSymbol, file: IrFile) {
        // TODO: These `findOrGenerate` calls generate a whole subtree.
        //  This a simple but clearly suboptimal solution.
        symbol.findCEnumDescriptor()?.let { enumDescriptor ->
            cEnumClassGenerator.findOrGenerateCEnum(enumDescriptor, file)
        }
        symbol.findCStructDescriptor()?.let { structDescriptor ->
            cStructClassGenerator.findOrGenerateCStruct(structDescriptor, file)
        }
    }

    /**
     * We postpone generation of bodies until IR linkage is complete.
     * This way we ensure that all used symbols are resolved.
     */
    fun generateBodies() {
        cEnumCompanionGenerator.invokePostLinkageSteps()
        cEnumByValueFunctionGenerator.invokePostLinkageSteps()
        cEnumClassGenerator.invokePostLinkageSteps()
        cEnumVarClassGenerator.invokePostLinkageSteps()
        cStructClassGenerator.invokePostLinkageSteps()
        cStructCompanionGenerator.invokePostLinkageSteps()
    }

    fun getDeclaration(descriptor: DeclarationDescriptor, idSignature: IdSignature, file: IrFile, symbolKind: BinarySymbolData.SymbolKind): IrSymbolOwner {
        return symbolTable.run {
            when (symbolKind) {
                BinarySymbolData.SymbolKind.CONSTRUCTOR_SYMBOL -> descriptorExtension.declareConstructorFromLinker(descriptor as ClassConstructorDescriptor, idSignature) { s: IrConstructorSymbol ->
                    generateIrIfNeeded(s, file)
                    s.owner
                }
                BinarySymbolData.SymbolKind.CLASS_SYMBOL -> descriptorExtension.declareClassFromLinker(descriptor as ClassDescriptor, idSignature) { s ->
                    generateIrIfNeeded(s, file)
                    s.owner
                }
                BinarySymbolData.SymbolKind.ENUM_ENTRY_SYMBOL -> descriptorExtension.declareEnumEntryFromLinker(descriptor as ClassDescriptor, idSignature) { s: IrEnumEntrySymbol ->
                    generateIrIfNeeded(s, file)
                    s.owner
                }
                BinarySymbolData.SymbolKind.FUNCTION_SYMBOL -> descriptorExtension.declareSimpleFunctionFromLinker(descriptor as FunctionDescriptor, idSignature) { s: IrSimpleFunctionSymbol ->
                    generateIrIfNeeded(s, file)
                    s.owner
                }
                BinarySymbolData.SymbolKind.PROPERTY_SYMBOL -> descriptorExtension.declarePropertyFromLinker(descriptor as PropertyDescriptor, idSignature) { s: IrPropertySymbol ->
                    generateIrIfNeeded(s, file)
                    s.owner
                }
                // TODO: better error message?
                else -> error("Unexpected symbol kind $symbolKind for sig $idSignature")
            }
        }
    }

    companion object {
        const val cTypeDefinitionsFileName = "CTypeDefinitions"
    }
}
