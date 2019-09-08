/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.konan.impl

import org.jetbrains.kotlin.builtins.functions.functionInterfacePackageFragmentProvider
import org.jetbrains.kotlin.contracts.ContractDeserializerImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.CompositePackageFragmentProvider
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.incremental.components.LookupTracker
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.konan.library.*
import org.jetbrains.kotlin.konan.library.resolver.PackageAccessedHandler
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.serialization.deserialization.*
import org.jetbrains.kotlin.serialization.konan.KonanDeserializedPackageFragmentsFactory
import org.jetbrains.kotlin.serialization.konan.KonanPackageFragment
import org.jetbrains.kotlin.serialization.konan.KonanSerializerProtocol
import org.jetbrains.kotlin.serialization.konan.NullFlexibleTypeDeserializer
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.storage.StorageManager
import org.jetbrains.kotlin.storage.getValue
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.firstNotNullResult

// FIXME(ddol): decouple and move interop-specific logic back to Kotlin/Native.
internal object KonanDeserializedPackageFragmentsFactoryImpl : KonanDeserializedPackageFragmentsFactory {

    override fun createDeserializedPackageFragments(
        library: KonanLibrary,
        packageFragmentNames: List<String>,
        moduleDescriptor: ModuleDescriptor,
        packageAccessedHandler: PackageAccessedHandler?,
        storageManager: StorageManager
    ) = packageFragmentNames.flatMap {
        val fqName = FqName(it)
        val parts = library.packageMetadataParts(it)
        parts.map { partName ->
            KonanPackageFragment(fqName, library, packageAccessedHandler, storageManager, moduleDescriptor, partName)
        }
    }

    override fun createSyntheticPackageFragments(
        library: KonanLibrary,
        deserializedPackageFragments: List<KonanPackageFragment>,
        moduleDescriptor: ModuleDescriptor
    ): List<PackageFragmentDescriptor> {

        if (!library.isInterop) return emptyList()

        val mainPackageFqName = library.packageFqName
            ?: error("Inconsistent manifest: interop library ${library.libraryName} should have `package` specified")
        val exportForwardDeclarations = library.exportForwardDeclarations

        val aliasedPackageFragments = deserializedPackageFragments.filter { it.fqName == mainPackageFqName }

        val result = mutableListOf<PackageFragmentDescriptor>()
        listOf(
            ForwardDeclarationsFqNames.cNamesStructs,
            ForwardDeclarationsFqNames.objCNamesClasses,
            ForwardDeclarationsFqNames.objCNamesProtocols
        ).mapTo(result) { fqName ->
            ClassifierAliasingPackageFragmentDescriptor(aliasedPackageFragments, moduleDescriptor, fqName)
        }

        result.add(ExportedForwardDeclarationsPackageFragmentDescriptor(moduleDescriptor, mainPackageFqName, exportForwardDeclarations))

        return result
    }

    override fun createPackageFragmentProvider(
        library: KonanLibrary,
        packageAccessedHandler: PackageAccessedHandler?,
        packageFragmentNames: List<String>,
        storageManager: StorageManager,
        moduleDescriptor: ModuleDescriptor,
        configuration: DeserializationConfiguration
    ): PackageFragmentProvider {

        val deserializedPackageFragments = createDeserializedPackageFragments(
            library, packageFragmentNames, moduleDescriptor, packageAccessedHandler, storageManager
        )

        val syntheticPackageFragments = createSyntheticPackageFragments(
            library, deserializedPackageFragments, moduleDescriptor
        )

        val packageFragmentProvider = PackageFragmentProviderImpl(deserializedPackageFragments + syntheticPackageFragments)

        val notFoundClasses = NotFoundClasses(storageManager, moduleDescriptor)

        val annotationAndConstantLoader = AnnotationAndConstantLoaderImpl(
            moduleDescriptor,
            notFoundClasses,
            KonanSerializerProtocol
        )

        val components = DeserializationComponents(
            storageManager,
            moduleDescriptor,
            configuration,
            DeserializedClassDataFinder(packageFragmentProvider),
            annotationAndConstantLoader,
            packageFragmentProvider,
            LocalClassifierTypeSettings.Default,
            ErrorReporter.DO_NOTHING,
            LookupTracker.DO_NOTHING,
            NullFlexibleTypeDeserializer,
            emptyList(),
            notFoundClasses,
            ContractDeserializerImpl(configuration, storageManager),
            extensionRegistryLite = KonanSerializerProtocol.extensionRegistry
        )

        for (packageFragment in deserializedPackageFragments) {
            packageFragment.initialize(components)
        }

        return if (library.uniqueName == KONAN_STDLIB_NAME)
            CompositePackageFragmentProvider(
                listOf(
                    packageFragmentProvider,
                    functionInterfacePackageFragmentProvider(storageManager, moduleDescriptor)
                )
            )
        else
            packageFragmentProvider
    }

    override fun createForwardDeclarationHackPackagePartProvider(
        storageManager: StorageManager,
        module: ModuleDescriptorImpl
    ): PackageFragmentProviderImpl {
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
        return packageFragmentProvider
    }
}

/**
 * The package fragment to export forward declarations from interop package namespace, i.e.
 * redirect "$pkg.$name" to e.g. "cnames.structs.$name".
 */
class ExportedForwardDeclarationsPackageFragmentDescriptor(
    module: ModuleDescriptor,
    fqName: FqName,
    declarations: List<FqName>
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        private val nameToFqName = declarations.map { it.shortName() to it }.toMap()

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? {
            val declFqName = nameToFqName[name] ?: return null

            val packageView = module.getPackage(declFqName.parent())
            return packageView.memberScope.getContributedClassifier(name, location) // ?: FIXME(ddol): delegate to forward declarations synthetic module!
        }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, " {")
            p.pushIndent()

            p.println("declarations = $declarations")

            p.popIndent()
            p.println("}")
        }

    }

    override fun getMemberScope() = memberScope
}

/**
 * The package fragment that redirects all requests for classifier lookup to its targets.
 */
class ClassifierAliasingPackageFragmentDescriptor(
    targets: List<KonanPackageFragment>,
    module: ModuleDescriptor,
    fqName: FqName
) : PackageFragmentDescriptorImpl(module, fqName) {

    private val memberScope = object : MemberScopeImpl() {

        override fun getContributedClassifier(name: Name, location: LookupLocation) =
            targets.firstNotNullResult {
                if (it.hasTopLevelClassifier(name)) {
                    it.getMemberScope().getContributedClassifier(name, location)
                } else {
                    null
                }
            }

        override fun printScopeStructure(p: Printer) {
            p.println(this::class.java.simpleName, " {")
            p.pushIndent()

            p.println("targets = $targets")

            p.popIndent()
            p.println("}")
        }
    }

    override fun getMemberScope(): MemberScope = memberScope
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