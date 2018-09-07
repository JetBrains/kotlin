/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan.impl

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.konan.KonanModuleOrigin
import org.jetbrains.kotlin.descriptors.konan.SyntheticModulesOrigin
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.konan.library.KonanLibrary
import org.jetbrains.kotlin.konan.library.resolver.KonanLibraryResolveResult
import org.jetbrains.kotlin.konan.library.resolver.PackageAccessedHandler
import org.jetbrains.kotlin.konan.util.profile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.konan.KonanDeserializedModuleDescriptorFactory
import org.jetbrains.kotlin.serialization.konan.KonanResolvedModuleDescriptors
import org.jetbrains.kotlin.serialization.konan.KonanResolvedModuleDescriptorsFactory
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.Printer

class KonanResolvedModuleDescriptorsFactoryImpl(
    override val moduleDescriptorFactory: KonanDeserializedModuleDescriptorFactory
) : KonanResolvedModuleDescriptorsFactory {

    override fun createResolved(
        resolvedLibraries: KonanLibraryResolveResult,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        languageVersionSettings: LanguageVersionSettings,
        customAction: ((KonanLibrary, ModuleDescriptorImpl) -> Unit)?,
        customCapabilitiesGenerator: ((KonanLibrary) -> Map<ModuleDescriptor.Capability<*>, Any?>)?
    ): KonanResolvedModuleDescriptors {

        val moduleDescriptors = mutableListOf<ModuleDescriptorImpl>()
        @Suppress("NAME_SHADOWING")
        var builtIns = builtIns

        // Build module descriptors.
        resolvedLibraries.forEach { library, packageAccessedHandler ->
            profile("Loading ${library.libraryName}") {

                val customCapabilities = customCapabilitiesGenerator?.invoke(library) ?: emptyMap()

                // MutableModuleContext needs ModuleDescriptorImpl, rather than ModuleDescriptor.
                val moduleDescriptor = createDescriptorOptionalBuiltsIns(
                    library, languageVersionSettings, storageManager, builtIns, packageAccessedHandler, customCapabilities
                )
                builtIns = moduleDescriptor.builtIns
                moduleDescriptors.add(moduleDescriptor)

                customAction?.invoke(library, moduleDescriptor)
            }
        }

        val forwardDeclarationsModule = createForwardDeclarationsModule(builtIns, storageManager)

        // Set inter-dependencies between module descriptors, add forwarding declarations module.
        for (module in moduleDescriptors) {
            // Yes, just to all of them.
            module.setDependencies(moduleDescriptors + forwardDeclarationsModule)
        }

        return KonanResolvedModuleDescriptors(moduleDescriptors, forwardDeclarationsModule)
    }

    private fun createForwardDeclarationsModule(
        builtIns: KotlinBuiltIns?,
        storageManager: StorageManager
    ): ModuleDescriptorImpl {

        val name = Name.special("<forward declarations>")
        val module = createDescriptorOptionalBuiltsIns(name, storageManager, builtIns, SyntheticModulesOrigin)

        fun createPackage(fqName: FqName, supertypeName: String, classKind: ClassKind) =
            ForwardDeclarationsPackageFragmentDescriptor(
                storageManager,
                module,
                fqName,
                Name.identifier(supertypeName),
                classKind
            )

        val packageFragmentProvider = PackageFragmentProviderImpl(
            listOf(
                createPackage(ForwardDeclarationsFqNames.cNamesStructs, "COpaque", ClassKind.CLASS),
                createPackage(ForwardDeclarationsFqNames.objCNamesClasses, "ObjCObjectBase", ClassKind.CLASS),
                createPackage(ForwardDeclarationsFqNames.objCNamesProtocols, "ObjCObject", ClassKind.INTERFACE)
            )
        )

        module.initialize(packageFragmentProvider)
        module.setDependencies(module)

        return module
    }

    private fun createDescriptorOptionalBuiltsIns(
        name: Name,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        moduleOrigin: KonanModuleOrigin
    ) = if (builtIns != null)
        moduleDescriptorFactory.descriptorFactory.createDescriptor(name, storageManager, builtIns, moduleOrigin)
    else
        moduleDescriptorFactory.descriptorFactory.createDescriptorAndNewBuiltIns(name, storageManager, moduleOrigin)

    private fun createDescriptorOptionalBuiltsIns(
        library: KonanLibrary,
        languageVersionSettings: LanguageVersionSettings,
        storageManager: StorageManager,
        builtIns: KotlinBuiltIns?,
        packageAccessedHandler: PackageAccessedHandler?,
        customCapabilities: Map<ModuleDescriptor.Capability<*>, Any?>
    ) = if (builtIns != null)
        moduleDescriptorFactory.createDescriptor(
            library,
            languageVersionSettings,
            storageManager,
            builtIns,
            packageAccessedHandler,
            customCapabilities
        )
    else
        moduleDescriptorFactory.createDescriptorAndNewBuiltIns(
            library,
            languageVersionSettings,
            storageManager,
            packageAccessedHandler,
            customCapabilities
        )
}

/**
 * Package fragment which creates descriptors for forward declarations on demand.
 */
private class ForwardDeclarationsPackageFragmentDescriptor(
    storageManager: StorageManager,
    module: ModuleDescriptor,
    fqName: FqName,
    supertypeName: Name,
    classKind: ClassKind
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        private val declarations = storageManager.createMemoizedFunction(this::createDeclaration)

        private val supertype by storageManager.createLazyValue {
            val descriptor = builtIns.builtInsModule.getPackage(ForwardDeclarationsFqNames.packageName)
                .memberScope
                .getContributedClassifier(supertypeName, NoLookupLocation.FROM_BACKEND) as ClassDescriptor

            descriptor.defaultType
        }

        private fun createDeclaration(name: Name): ClassDescriptor {
            return ClassDescriptorImpl(
                this@ForwardDeclarationsPackageFragmentDescriptor,
                name,
                Modality.FINAL,
                classKind,
                listOf(supertype),
                SourceElement.NO_SOURCE,
                false,
                LockBasedStorageManager.NO_LOCKS
            ).apply {
                this.initialize(MemberScope.Empty, emptySet(), null)
            }
        }

        override fun getContributedClassifier(name: Name, location: LookupLocation) = declarations(name)

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, "{}")
        }
    }

    override fun getMemberScope(): MemberScope = memberScope
}

// FIXME(ddol): decouple and move interop-specific logic back to Kotlin/Native.
internal object ForwardDeclarationsFqNames {

    val packageName = FqName("kotlinx.cinterop")

    val cNames = FqName("cnames")
    val cNamesStructs = cNames.child(Name.identifier("structs"))

    val objCNames = FqName("objcnames")
    val objCNamesClasses = objCNames.child(Name.identifier("classes"))
    val objCNamesProtocols = objCNames.child(Name.identifier("protocols"))
}
