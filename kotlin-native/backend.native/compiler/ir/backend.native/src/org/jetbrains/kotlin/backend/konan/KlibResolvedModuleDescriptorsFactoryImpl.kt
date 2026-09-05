/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.library.metadata.impl

import org.jetbrains.kotlin.K1Deprecation
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.*
import org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
import org.jetbrains.kotlin.library.metadata.isCInteropLibrary
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.NativeForwardDeclarationKind
import org.jetbrains.kotlin.name.NativeStandardInteropNames
import org.jetbrains.kotlin.platform.konan.NativePlatforms
import org.jetbrains.kotlin.resolve.ImplicitIntegerCoercion
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.util.profile
import org.jetbrains.kotlin.utils.Printer
import java.nio.file.Path

class KotlinResolvedModuleDescriptors(
        /**
         * The list of modules each representing an individual Kotlin/Native library. All modules
         * in this list have properly installed dependencies, i.e. module has all necessary dependencies
         * on other modules plus a dependency on the [forwardDeclarationsModule].
         */
        val resolvedDescriptors: List<ModuleDescriptorImpl>,

        /**
         * This is a module which "contains" forward declarations.
         * Note: this module should be unique per compilation and should always be the last dependency of any module.
         */
        val forwardDeclarationsModule: ModuleDescriptorImpl,

        val friendModules: Set<ModuleDescriptorImpl>,
        val refinesModules: Set<ModuleDescriptorImpl>
)

@K1Deprecation
class KlibResolvedModuleDescriptorsFactoryImpl(
        val moduleDescriptorFactory: KlibMetadataModuleDescriptorFactory
) {

    /**
     * Given the [libraries] creates the list of [ModuleDescriptorImpl]s with properly installed
     * inter-dependencies. The result of this method is returned in a form of [KotlinResolvedModuleDescriptors] instance.
     *
     * Please use this method with care: Unless this method accepts `null` for [builtIns], it is not recommended to
     * invoke it this way. If you are compiling a source module, please supply the non-null [builtIns] from the
     * source module, so that all modules created in your compilation session will share the same built-ins instance.
     *
     * Otherwise (if `null` was supplied), a new instance of [KotlinBuiltIns] will be created. The created built-ins
     * instance will be shared by all modules created in this method. But this instance will have no connection
     * with probably existing built-ins instance of your source module(s).
     *
     * FYI: No much attention to naming of this function, anyway it's going to be removed soon as a part of K1.
     */
    fun createResolved2(
            libraries: List<KotlinLibrary>,
            storageManager: StorageManager,
            builtIns: KotlinBuiltIns?,
            languageVersionSettings: LanguageVersionSettings,
            friendModuleFiles: Set<Path>,
            refinesModuleFiles: Set<Path>,
            includedLibraryFiles: Set<Path>,
            additionalDependencyModules: Iterable<ModuleDescriptorImpl>,
            isForMetadataCompilation: Boolean,
    ): KotlinResolvedModuleDescriptors {

        val moduleDescriptors = mutableListOf<ModuleDescriptorImpl>()

        @Suppress("NAME_SHADOWING")
        var builtIns = builtIns

        val friendModuleDescriptors = mutableSetOf<ModuleDescriptorImpl>()
        val refinesModuleDescriptors = mutableSetOf<ModuleDescriptorImpl>()
        val includedLibraryDescriptors = mutableSetOf<ModuleDescriptorImpl>()

        // Build module descriptors.
        libraries.forEach { library ->
            profile("Loading ${library.path}") {

                // MutableModuleContext needs ModuleDescriptorImpl, rather than ModuleDescriptor.
                val moduleDescriptor = createDescriptorOptionalBuiltsIns(
                        library, languageVersionSettings, storageManager, builtIns,
                )
                builtIns = moduleDescriptor.builtIns
                moduleDescriptors.add(moduleDescriptor)

                if (refinesModuleFiles.contains(library.path))
                    refinesModuleDescriptors.add(moduleDescriptor)
                if (friendModuleFiles.contains(library.path))
                    friendModuleDescriptors.add(moduleDescriptor)
                if (includedLibraryFiles.contains(library.path))
                    includedLibraryDescriptors.add(moduleDescriptor)
            }
        }

        val forwardDeclarationsModule = createForwardDeclarationsModule(
                builtIns,
                storageManager,
                // If we are compiling metadata, make synthetic forward declarations `expect`,
                // because otherwise `getFirstClassifierDiscriminateHeaders` would prefer it over a
                // "real" `expect` declaration from a commonized interop library, which would ruin
                // the whole idea of using synthetic forward declarations only when no proper definitions
                // are found.
                //
                // If we are compiling for the actual native platform, continue using non-expect
                // forward declarations (to prevent getting non-actualized expects into the backend,
                // and to prevent related klib signature changes).
                isExpect = isForMetadataCompilation,
        )

        // Set inter-dependencies between module descriptors, add forwarding declarations module.
        val additionalDependencyModulesCopy = additionalDependencyModules.toSet()
        val friendsForNonIncludedModule = additionalDependencyModulesCopy
        val friendsForIncludedModule = buildSet<ModuleDescriptorImpl> {
            this += friendsForNonIncludedModule
            this += friendModuleDescriptors
            this += refinesModuleDescriptors
        }
        val allDependencies = moduleDescriptors + additionalDependencyModulesCopy + forwardDeclarationsModule
        for (module in moduleDescriptors) {
            val friends = if (module in includedLibraryDescriptors) {
                friendsForIncludedModule
            } else {
                friendsForNonIncludedModule
            }

            // Yes, just to all of them.
            module.setDependencies(allDependencies, friends)
        }

        return KotlinResolvedModuleDescriptors(
                resolvedDescriptors = moduleDescriptors,
                forwardDeclarationsModule = forwardDeclarationsModule,
                friendModules = friendModuleDescriptors,
                refinesModules = refinesModuleDescriptors
        )
    }

    private fun createForwardDeclarationsModule(
            builtIns: KotlinBuiltIns?,
            storageManager: StorageManager,
            isExpect: Boolean
    ): ModuleDescriptorImpl {

        val module = createDescriptorOptionalBuiltsIns(FORWARD_DECLARATIONS_MODULE_NAME, storageManager, builtIns, SyntheticModulesOrigin)

        fun createPackage(forwardDeclarationKind: NativeForwardDeclarationKind) =
                ForwardDeclarationsPackageFragmentDescriptor(
                        storageManager,
                        module,
                        forwardDeclarationKind.packageFqName,
                        forwardDeclarationKind.superClassName,
                        forwardDeclarationKind.classKind,
                        isExpect
                )

        val packageFragmentProvider = PackageFragmentProviderImpl(
                NativeForwardDeclarationKind.entries.map { createPackage(it) }
        )

        module.initialize(packageFragmentProvider)
        module.setDependencies(module)

        return module
    }

    private fun createDescriptorOptionalBuiltsIns(
            name: Name,
            storageManager: StorageManager,
            builtIns: KotlinBuiltIns?,
            moduleOrigin: KlibModuleOrigin
    ): ModuleDescriptorImpl {
        val builtInsToUse = builtIns ?: moduleDescriptorFactory.createBuiltIns(storageManager)
        val moduleDescriptor = ModuleDescriptorImpl(
                name,
                storageManager,
                builtInsToUse,
                capabilities = mapOf(
                        KlibModuleOrigin.CAPABILITY to moduleOrigin,
                        ImplicitIntegerCoercion.MODULE_CAPABILITY to moduleOrigin.isCInteropLibrary()
                ),
                // TODO: don't use hardcoded platform; it should be supplied as a parameter
                platform = NativePlatforms.unspecifiedNativePlatform
        )

        if (builtIns == null) {
            builtInsToUse.builtInsModule = moduleDescriptor
        }

        return moduleDescriptor
    }

    private fun createDescriptorOptionalBuiltsIns(
            library: KotlinLibrary,
            languageVersionSettings: LanguageVersionSettings,
            storageManager: StorageManager,
            builtIns: KotlinBuiltIns?,
    ): ModuleDescriptorImpl = if (builtIns != null)
        moduleDescriptorFactory.createDescriptor(library, languageVersionSettings, storageManager, builtIns)
    else
        moduleDescriptorFactory.createDescriptorAndNewBuiltIns(library, languageVersionSettings, storageManager)
}

/**
 * Package fragment which creates descriptors for forward declarations on demand.
 */
@K1Deprecation
class ForwardDeclarationsPackageFragmentDescriptor(
        storageManager: StorageManager,
        module: ModuleDescriptor,
        fqName: FqName,
        supertypeName: Name,
        classKind: ClassKind,
        isExpect: Boolean
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        private val declarations: (Name) -> ClassDescriptor = storageManager.createMemoizedFunction(this::createDeclaration)

        private val supertype by storageManager.createLazyValue {
            findCinteropClass(supertypeName).defaultType
        }

        /**
         * Normally, this can't be null. But it's possible inside IDE, if one uses new IDE with
         * old compiler in a project. In that case, IDE would try to generate synthetic declaration
         * but would fail to find annotation class.
         *
         * A better way to do this would be introducing language feature, but unfortunately, we can't
         * do it between 1.9.0 and 1.9.20.
         */
        private val experimentalAnnotationType by storageManager.createNullableLazyValue {
            findCinteropClassOrNull(NativeStandardInteropNames.ExperimentalForeignApi)?.defaultType
        }

        private fun findCinteropClass(name: Name): ClassDescriptor = findCinteropClassOrNull(name) ?: error("Class $name is not found")

        private fun findCinteropClassOrNull(name: Name): ClassDescriptor? {
            return builtIns.builtInsModule.getPackage(NativeStandardInteropNames.cInteropPackage)
                    .memberScope
                    .getContributedClassifier(name, NoLookupLocation.FROM_BACKEND) as ClassDescriptor?
        }

        private fun createDeclaration(name: Name): ClassDescriptor {
            val experimentalAnnotation = experimentalAnnotationType?.let {
                AnnotationDescriptorImpl(
                        it,
                        emptyMap(),
                        SourceElement.NO_SOURCE
                )
            }

            return object : ClassDescriptorImpl(
                    this@ForwardDeclarationsPackageFragmentDescriptor,
                    name,
                    Modality.FINAL,
                    classKind,
                    listOf(supertype),
                    SourceElement.NO_SOURCE,
                    false,
                    LockBasedStorageManager.NO_LOCKS
            ) {
                override fun isExpect(): Boolean = isExpect
                override val annotations: Annotations = Annotations.create(listOfNotNull(experimentalAnnotation))
            }.apply {
                this.initialize(MemberScope.Empty, emptySet(), null)
            }
        }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor = declarations(name)

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, "{}")
        }
    }

    override fun getMemberScope(): MemberScope = memberScope
}
