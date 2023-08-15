/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializerKind
import org.jetbrains.kotlin.backend.common.serialization.KotlinIrLinker
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.konan.descriptors.isForwardDeclarationModule
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOriginImpl
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.resolve.descriptorUtil.module

internal class KonanForwardDeclarationModuleDeserializer(
    moduleDescriptor: ModuleDescriptor,
    private val linker: KotlinIrLinker,
    private val stubGenerator: DeclarationStubGenerator,
) : IrModuleDeserializer(moduleDescriptor, KotlinAbiVersion.CURRENT) {
    init {
        require(moduleDescriptor.isForwardDeclarationModule)
    }

    companion object {
        private val FORWARD_DECLARATION_ORIGIN = object : IrDeclarationOriginImpl("FORWARD_DECLARATION_ORIGIN") {}
    }

    private val declaredDeclaration = mutableMapOf<IdSignature, IrClass>()

    private fun IdSignature.isForwardDeclarationSignature(): Boolean {
        if (isPubliclyVisible) {
            return packageFqName() in NativeForwardDeclarationKind.packageFqNameToKind
        }

        return false
    }

    override fun contains(idSig: IdSignature): Boolean = idSig.isForwardDeclarationSignature()

    private fun resolveDescriptor(idSig: IdSignature): ClassDescriptor? =
            with(idSig as IdSignature.CommonSignature) {
                val classId = ClassId(packageFqName(), FqName(declarationFqName), false)
                moduleDescriptor.findClassAcrossModuleDependencies(classId)
            }

    private fun buildForwardDeclarationStub(descriptor: ClassDescriptor): IrClass {
        return stubGenerator.generateClassStub(descriptor).also {
            it.origin = FORWARD_DECLARATION_ORIGIN
        }
    }

    override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
        require(symbolKind == BinarySymbolData.SymbolKind.CLASS_SYMBOL) {
            "Only class could be a Forward declaration $idSig (kind $symbolKind)"
        }
        val descriptor = resolveDescriptor(idSig) ?: return null
        val actualModule = descriptor.module
        if (actualModule !== moduleDescriptor) {
            val moduleDeserializer = linker.resolveModuleDeserializer(actualModule, idSig)
            moduleDeserializer.addModuleReachableTopLevel(idSig)
            return linker.symbolTable.referenceClass(idSig)
        }

        return declaredDeclaration.getOrPut(idSig) { buildForwardDeclarationStub(descriptor) }.symbol
    }

    override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing = error("No descriptor found for $idSig")

    override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor, linker.builtIns)
    override val moduleDependencies: Collection<IrModuleDeserializer> = emptyList()

    override val kind get() = IrModuleDeserializerKind.SYNTHETIC
}