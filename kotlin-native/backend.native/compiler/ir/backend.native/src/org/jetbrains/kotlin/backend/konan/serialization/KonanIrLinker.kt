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

import org.jetbrains.kotlin.backend.common.linkage.issues.UserVisibleIrModulesSupport
import org.jetbrains.kotlin.backend.common.linkage.partial.PartialLinkageSupportForLinker
import org.jetbrains.kotlin.backend.common.overrides.IrLinkerFakeOverrideProvider
import org.jetbrains.kotlin.backend.common.serialization.*
import org.jetbrains.kotlin.backend.konan.ir.interop.IrProviderForCEnumAndCStructStubs
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.konan.isNativeStdlib
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.overrides.IrExternalOverridabilityCondition
import org.jetbrains.kotlin.ir.types.IrTypeSystemContextImpl
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl.Companion.FORWARD_DECLARATIONS_MODULE_NAME
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary

@OptIn(ObsoleteDescriptorBasedAPI::class)
internal class KonanIrLinker(
        private val currentModule: ModuleDescriptor,
        messageCollector: MessageCollector,
        builtIns: IrBuiltIns,
        symbolTable: SymbolTable,
        friendModules: Map<String, Collection<String>>,
        private val forwardModuleDescriptor: ModuleDescriptor?,
        private val stubGenerator: DeclarationStubGenerator,
        private val cenumsProvider: IrProviderForCEnumAndCStructStubs,
        exportedDependencies: List<ModuleDescriptor>,
        override val partialLinkageSupport: PartialLinkageSupportForLinker,
        private val nativeCacheSupport: NativeCacheSupport,
        override val userVisibleIrModulesSupport: UserVisibleIrModulesSupport,
        externalOverridabilityConditions: List<IrExternalOverridabilityCondition>,
) : KotlinIrLinker(currentModule, messageCollector, builtIns, symbolTable, exportedDependencies) {
    override fun isBuiltInModule(moduleDescriptor: ModuleDescriptor): Boolean = moduleDescriptor.isNativeStdlib()

    private val forwardDeclarationDeserializer = forwardModuleDescriptor?.let {
        KonanForwardDeclarationModuleDeserializer(it, this, stubGenerator)
    }

    override val fakeOverrideBuilder = IrLinkerFakeOverrideProvider(
            linker = this,
            symbolTable = symbolTable,
            mangler = KonanManglerIr,
            typeSystem = IrTypeSystemContextImpl(builtIns),
            friendModules = friendModules,
            partialLinkageSupport = partialLinkageSupport,
            platformSpecificClassFilter = KonanFakeOverrideClassFilter,
            externalOverridabilityConditions = externalOverridabilityConditions,
    )

    val moduleDeserializers = mutableMapOf<ModuleDescriptor, KonanPartialModuleDeserializer>()
    val klibToModuleDeserializerMap = mutableMapOf<KotlinLibrary, KonanPartialModuleDeserializer>()

    fun getCachedDeclarationModuleDeserializer(declaration: IrDeclaration): KonanPartialModuleDeserializer? {
        return nativeCacheSupport.getDescriptorForCachedDeclarationModuleDeserializer(declaration)?.let {
            moduleDeserializers[it] ?: error("No module deserializer for ${declaration.render()}")
        }
    }

    override fun createModuleDeserializer(moduleDescriptor: ModuleDescriptor, klib: KotlinLibrary?, strategyResolver: (String) -> DeserializationStrategy) =
            when {
                moduleDescriptor === forwardModuleDescriptor -> {
                    forwardDeclarationDeserializer ?: error("forward declaration deserializer expected")
                }
                klib == null -> {
                    error("Expecting kotlin library for $moduleDescriptor")
                }
                klib.isCInteropLibrary() -> {
                    KonanInteropModuleDeserializer(
                            moduleDescriptor,
                            klib,
                            listOfNotNull(forwardDeclarationDeserializer),
                            nativeCacheSupport.cachedLibraries.isLibraryCached(klib),
                            cenumsProvider,
                            stubGenerator,
                            builtIns
                    )
                }
                else -> {
                    val deserializationStrategy = nativeCacheSupport.getDeserializationStrategy(klib)
                    KonanPartialModuleDeserializer(
                            this, moduleDescriptor, klib, stubGenerator, nativeCacheSupport.cachedLibraries, inlineFunctionFiles, strategyResolver, deserializationStrategy
                    ).also {
                        moduleDeserializers[moduleDescriptor] = it
                        klibToModuleDeserializerMap[klib] = it
                    }
                }
            }

    override fun postProcess(inOrAfterLinkageStep: Boolean) {
        stubGenerator.unboundSymbolGeneration = true
        super.postProcess(inOrAfterLinkageStep)
    }

    private val inlineFunctionFiles = mutableMapOf<IrExternalPackageFragment, IrFile>()

    override fun getFileOf(declaration: IrDeclaration): IrFile {
        val packageFragment = declaration.getPackageFragment()
        return packageFragment as? IrFile
                ?: inlineFunctionFiles[packageFragment as IrExternalPackageFragment]
                ?: error("Unknown external package fragment: ${packageFragment.packageFragmentDescriptor}")
    }

    fun getExternalDeclarationFileName(declaration: IrDeclaration) = when (val packageFragment = declaration.getPackageFragment()) {
        is IrFile -> packageFragment.path

        is IrExternalPackageFragment -> {
            val moduleDescriptor = packageFragment.moduleDescriptor
            val moduleDeserializer = moduleDeserializers[moduleDescriptor] ?: error("No module deserializer for $moduleDescriptor")
            moduleDeserializer.getFileNameOf(declaration)
        }

        else -> error("Unknown package fragment kind ${packageFragment::class.java}")
    }

    private val String.isForwardDeclarationModuleName: Boolean get() = this == FORWARD_DECLARATIONS_MODULE_NAME.asString()

    val modules: Map<String, IrModuleFragment>
        get() = mutableMapOf<String, IrModuleFragment>().apply {
            deserializersForModules
                    .filter { !it.key.isForwardDeclarationModuleName && it.value.moduleDescriptor !== currentModule }
                    .forEach {
                        val klib = it.value.klib as? KotlinLibrary ?: error("Expected to be KotlinLibrary (${it.key})")
                        this[klib.libraryName] = it.value.moduleFragment
                    }
        }
}
