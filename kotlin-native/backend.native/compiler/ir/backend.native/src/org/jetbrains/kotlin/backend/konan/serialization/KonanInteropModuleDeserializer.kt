/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.serialization.DescriptorByIdSignatureFinderImpl
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializer
import org.jetbrains.kotlin.backend.common.serialization.IrModuleDeserializerKind
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.konan.ir.interop.IrProviderForCEnumAndCStructStubs
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.findPackage
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSymbolOwner
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrModuleFragmentImpl
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.util.DeclarationStubGenerator
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.NaiveSourceBasedFileEntryImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.library.KotlinAbiVersion
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.name.NativeStandardInteropNames
import org.jetbrains.kotlin.psi2ir.lazy.IrLazyClass

internal class KonanInteropModuleDeserializer(
    moduleDescriptor: ModuleDescriptor,
    override val klib: KotlinLibrary,
    override val moduleDependencies: Collection<IrModuleDeserializer>,
    private val isLibraryCached: Boolean,
    private val cenumsProvider: IrProviderForCEnumAndCStructStubs,
    private val stubGenerator: DeclarationStubGenerator,
) : IrModuleDeserializer(moduleDescriptor, klib.versions.abiVersion ?: KotlinAbiVersion.CURRENT) {
    init {
        require(klib.isCInteropLibrary())
    }

    private val descriptorByIdSignatureFinder = DescriptorByIdSignatureFinderImpl(
        moduleDescriptor, KonanManglerDesc,
        DescriptorByIdSignatureFinderImpl.LookupMode.MODULE_ONLY
    )

    private fun IdSignature.isInteropSignature() = IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.test()

    override fun contains(idSig: IdSignature): Boolean {
        if (idSig.isPubliclyVisible) {
            if (idSig.isInteropSignature()) {
                // TODO: add descriptor cache??
                return descriptorByIdSignatureFinder.findDescriptorBySignature(idSig) != null
            }
        }

        return false
    }

    private fun DeclarationDescriptor.isCEnumsOrCStruct(): Boolean = cenumsProvider.isCEnumOrCStruct(this)

    private val fileMap = mutableMapOf<PackageFragmentDescriptor, IrFile>()

    private fun getIrFile(packageFragment: PackageFragmentDescriptor): IrFile = fileMap.getOrPut(packageFragment) {
        IrFileImpl(
            NaiveSourceBasedFileEntryImpl(NativeStandardInteropNames.cTypeDefinitionsFileName),
            packageFragment,
            moduleFragment
        ).also {
            moduleFragment.files.add(it)
        }
    }

    private fun resolveCEnumsOrStruct(descriptor: DeclarationDescriptor, idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
        val file = getIrFile(descriptor.findPackage())
        return cenumsProvider.getDeclaration(descriptor, idSig, file, symbolKind).symbol
    }

    override fun tryDeserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol? {
        val descriptor = descriptorByIdSignatureFinder.findDescriptorBySignature(idSig) ?: return null
        // If library is cached we don't need to create an IrClass for struct or enum.
        if (!isLibraryCached && descriptor.isCEnumsOrCStruct()) return resolveCEnumsOrStruct(descriptor, idSig, symbolKind)

        val symbolOwner = stubGenerator.generateMemberStub(descriptor) as IrSymbolOwner

        // Workaround for KT-83236: touch all types accessible via lazy class stub generated in the previous line
        // to trigger its deserialization. A better idea might be to generate a non-lazy stub.
        (symbolOwner as? IrLazyClass)?.let { owner ->
            fun touchTypes(function: IrFunction) {
                function.returnType
                function.typeParameters.forEach { it.superTypes }
                function.parameters.forEach { it.type }
                // Don't touch `overriddenSymbols` here, due to an error in the usecase "a lazy class with a real class in super-type":
                // If the supertype is not found, stubGenerator incorrectly would not request symbols through the linker, but instead generates them in place.
                // Later the real class will be correctly attempted to be deserialized, which will fail with "symbol already bound".
            }

            owner.superTypes
            // No need to touch properties as well, since all relevant types would be touched via types of separate accessor functions.
            for (function in owner.functions) {
                touchTypes(function)
            }
            for (constructor in owner.constructors) {
                // Now cinterop constructors are duplicated with deprecated `init` functions, see `ObjCMethodStubBuilder.build()`.
                // They might be removed, so explicit constructors processing is required here to be on the safe side.
                touchTypes(constructor)
            }
        }

        return symbolOwner.symbol
    }

    override fun deserializedSymbolNotFound(idSig: IdSignature): Nothing = error("No descriptor found for $idSig")

    override val moduleFragment: IrModuleFragment = IrModuleFragmentImpl(moduleDescriptor)

    override val kind get() = IrModuleDeserializerKind.DESERIALIZED
}