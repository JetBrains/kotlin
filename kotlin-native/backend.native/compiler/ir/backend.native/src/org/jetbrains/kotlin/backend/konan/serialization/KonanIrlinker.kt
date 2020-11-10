/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.konan.serialization

import org.jetbrains.kotlin.backend.common.LoggingContext
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideBuilder
import org.jetbrains.kotlin.backend.common.overrides.FakeOverrideClassFilter
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.common.serialization.encodings.BinarySymbolData
import org.jetbrains.kotlin.backend.common.serialization.signature.IdSignatureSerializer
import org.jetbrains.kotlin.backend.konan.CachedLibraries
import org.jetbrains.kotlin.backend.konan.descriptors.isInteropLibrary
import org.jetbrains.kotlin.backend.konan.ir.interop.IrProviderForCEnumAndCStructStubs
import org.jetbrains.kotlin.backend.konan.ir.konanLibrary
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.konan.DeserializedKlibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.descriptors.konan.klibModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.kotlinLibrary
import org.jetbrains.kotlin.ir.builders.TranslationPluginContext
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.descriptors.IrAbstractFunctionFactory
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrPublicSymbolBase
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementTransformer
import org.jetbrains.kotlin.ir.visitors.IrElementVisitor
import org.jetbrains.kotlin.library.IrLibrary
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.descriptorUtil.module

object KonanFakeOverrideClassFilter : FakeOverrideClassFilter {
    private fun IdSignature.isInteropSignature(): Boolean = with(this) {
        IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.test()
    }

    // This is an alternative to .isObjCClass that doesn't need to walk up all the class heirarchy,
    // rather it only looks at immediate super class symbols.
    private fun IrClass.hasInteropSuperClass() = this.superTypes
        .mapNotNull { it.classOrNull }
        .filter { it is IrPublicSymbolBase<*> }
        .any { it.signature?.isInteropSignature() ?: false }

    override fun needToConstructFakeOverrides(clazz: IrClass): Boolean {
        return !clazz.hasInteropSuperClass()
    }
}

internal class KonanIrLinker(
        private val currentModule: ModuleDescriptor,
        override val functionalInterfaceFactory: IrAbstractFunctionFactory,
        override val translationPluginContext: TranslationPluginContext?,
        logger: LoggingContext,
        builtIns: IrBuiltIns,
        symbolTable: SymbolTable,
        private val forwardModuleDescriptor: ModuleDescriptor?,
        private val stubGenerator: DeclarationStubGenerator,
        private val cenumsProvider: IrProviderForCEnumAndCStructStubs,
        exportedDependencies: List<ModuleDescriptor>,
        deserializeFakeOverrides: Boolean,
        private val cachedLibraries: CachedLibraries
) : KotlinIrLinker(currentModule, logger, builtIns, symbolTable, exportedDependencies, deserializeFakeOverrides) {

    companion object {
        private val C_NAMES_NAME = Name.identifier("cnames")
        private val OBJC_NAMES_NAME = Name.identifier("objcnames")

        val FORWARD_DECLARATION_ORIGIN = object : IrDeclarationOriginImpl("FORWARD_DECLARATION_ORIGIN") {}

        const val offset = SYNTHETIC_OFFSET
    }

    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean = moduleDescriptor.isNativeStdlib()

    private val forwardDeclarationDeserializer = forwardModuleDescriptor?.let { KonanForwardDeclarationModuleDeserializer(it) }
    override val fakeOverrideBuilder = FakeOverrideBuilder(this, symbolTable, IdSignatureSerializer(KonanManglerIr), builtIns, KonanFakeOverrideClassFilter)

    override fun createModuleDeserializer(moduleDescriptor: ModuleDescriptor, klib: IrLibrary?, strategy: DeserializationStrategy): IrModuleDeserializer {
        if (moduleDescriptor === forwardModuleDescriptor) {
            return forwardDeclarationDeserializer ?: error("forward declaration deserializer expected")
        }

        if (klib is KotlinLibrary && klib.isInteropLibrary()) {
            val isCached = cachedLibraries.isLibraryCached(klib)
            return KonanInteropModuleDeserializer(moduleDescriptor, isCached)
        }

        return KonanModuleDeserializer(moduleDescriptor, klib ?: error("Expecting kotlin library"), strategy)
    }

    private inner class KonanModuleDeserializer(
            moduleDescriptor: ModuleDescriptor,
            klib: IrLibrary,
            strategy: DeserializationStrategy
    ): KotlinIrLinker.BasicIrModuleDeserializer(moduleDescriptor, klib, strategy) {
        override val moduleFragment: IrModuleFragment = KonanIrModuleFragmentImpl(moduleDescriptor, builtIns, emptyList())
    }

    private inner class KonanInteropModuleDeserializer(
            moduleDescriptor: ModuleDescriptor,
            private val isLibraryCached: Boolean
    ) : IrModuleDeserializer(moduleDescriptor) {
        init {
            assert(moduleDescriptor.kotlinLibrary.isInteropLibrary())
        }

        private val descriptorByIdSignatureFinder = DescriptorByIdSignatureFinder(
                moduleDescriptor, KonanManglerDesc,
                DescriptorByIdSignatureFinder.LookupMode.MODULE_ONLY
        )
        private fun IdSignature.isInteropSignature(): Boolean = IdSignature.Flags.IS_NATIVE_INTEROP_LIBRARY.test()

        override fun contains(idSig: IdSignature): Boolean {
            if (idSig.isPublic) {
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
            IrFileImpl(NaiveSourceBasedFileEntryImpl(IrProviderForCEnumAndCStructStubs.cTypeDefinitionsFileName), packageFragment).also {
                moduleFragment.files.add(it)
            }
        }

        private fun resolveCEnumsOrStruct(descriptor: DeclarationDescriptor, idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
            val file = getIrFile(descriptor.findPackage())
            return cenumsProvider.getDeclaration(descriptor, idSig, file, symbolKind).symbol
        }

        override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
            val descriptor = descriptorByIdSignatureFinder.findDescriptorBySignature(idSig) ?: error("Expecting descriptor for $idSig")
            // If library is cached we don't need to create an IrClass for struct or enum.
            if (!isLibraryCached && descriptor.isCEnumsOrCStruct()) return resolveCEnumsOrStruct(descriptor, idSig, symbolKind)

            val symbolOwner = stubGenerator.generateMemberStub(descriptor) as IrSymbolOwner

            return symbolOwner.symbol
        }

        override val moduleFragment: IrModuleFragment = KonanIrModuleFragmentImpl(moduleDescriptor, builtIns)
        override val moduleDependencies: Collection<IrModuleDeserializer> = listOfNotNull(forwardDeclarationDeserializer)
    }

    private inner class KonanForwardDeclarationModuleDeserializer(moduleDescriptor: ModuleDescriptor) : IrModuleDeserializer(moduleDescriptor) {
        init {
            assert(moduleDescriptor.isForwardDeclarationModule)
        }

        private val declaredDeclaration = mutableMapOf<IdSignature, IrClass>()

        private fun IdSignature.isForwardDeclarationSignature(): Boolean {
            if (isPublic) {
                return packageFqName().run {
                    startsWith(C_NAMES_NAME) || startsWith(OBJC_NAMES_NAME)
                }
            }

            return false
        }

        override fun contains(idSig: IdSignature): Boolean = idSig.isForwardDeclarationSignature()

        private fun resolveDescriptor(idSig: IdSignature): ClassDescriptor =
            with(idSig as IdSignature.PublicSignature) {
                val classId = ClassId(packageFqName(), FqName(declarationFqName), false)
                moduleDescriptor.findClassAcrossModuleDependencies(classId) ?: error("No declaration found with $idSig")
            }

        private fun buildForwardDeclarationStub(descriptor: ClassDescriptor): IrClass {
            return stubGenerator.generateClassStub(descriptor).also {
                it.origin = FORWARD_DECLARATION_ORIGIN
            }
        }

        override fun deserializeIrSymbol(idSig: IdSignature, symbolKind: BinarySymbolData.SymbolKind): IrSymbol {
            assert(symbolKind == BinarySymbolData.SymbolKind.CLASS_SYMBOL) { "Only class could be a Forward declaration $idSig (kind $symbolKind)" }
            val descriptor = resolveDescriptor(idSig)
            val actualModule = descriptor.module
            if (actualModule !== moduleDescriptor) {
                val moduleDeserializer = deserializersForModules[actualModule] ?: error("No module deserializer for $actualModule")
                moduleDeserializer.addModuleReachableTopLevel(idSig)
                return symbolTable.referenceClassFromLinker(descriptor, idSig)
            }

            return declaredDeclaration.getOrPut(idSig) { buildForwardDeclarationStub(descriptor) }.symbol
        }

        override val moduleFragment: IrModuleFragment = KonanIrModuleFragmentImpl(moduleDescriptor, builtIns)
        override val moduleDependencies: Collection<IrModuleDeserializer> = emptyList()
    }

    val modules: Map<String, IrModuleFragment>
        get() = mutableMapOf<String, IrModuleFragment>().apply {
            deserializersForModules
                    .filter { !it.key.isForwardDeclarationModule && it.value.moduleDescriptor !== currentModule }
                    .forEach { this.put(it.key.konanLibrary!!.libraryName, it.value.moduleFragment) }
        }
    class KonanPluginContext(
            override val moduleDescriptor: ModuleDescriptor,
            override val bindingContext: BindingContext,
            override val symbolTable: ReferenceSymbolTable,
            override val typeTranslator: TypeTranslator,
            override val irBuiltIns: IrBuiltIns
    ):TranslationPluginContext
}

class KonanIrModuleFragmentImpl(
        override val descriptor: ModuleDescriptor,
        override val irBuiltins: IrBuiltIns,
        files: List<IrFile> = emptyList(),
) : IrModuleFragment() {
    override val name: Name get() = descriptor.name // TODO

    override val files: MutableList<IrFile> = files.toMutableList()

    val konanLibrary = (descriptor.klibModuleOrigin as? DeserializedKlibModuleOrigin)?.library

    override fun <R, D> accept(visitor: IrElementVisitor<R, D>, data: D): R =
            visitor.visitModuleFragment(this, data)

    override fun <D> acceptChildren(visitor: IrElementVisitor<Unit, D>, data: D) {
        files.forEach { it.accept(visitor, data) }
    }

    override fun <D> transformChildren(transformer: IrElementTransformer<D>, data: D) {
        files.forEachIndexed { i, irFile ->
            files[i] = irFile.transform(transformer, data)
        }
    }
}

fun IrModuleFragment.toKonanModule() = KonanIrModuleFragmentImpl(descriptor, irBuiltins, files)

class KonanFileMetadataSource(val module: KonanIrModuleFragmentImpl) : MetadataSource.File {
    override val name: Name? = null
}