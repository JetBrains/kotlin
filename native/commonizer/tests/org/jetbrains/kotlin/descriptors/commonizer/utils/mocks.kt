/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.utils

import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataMonolithicSerializer
import org.jetbrains.kotlin.backend.common.serialization.metadata.KlibMetadataVersion
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.*
import org.jetbrains.kotlin.descriptors.commonizer.ModulesProvider.ModuleInfo
import org.jetbrains.kotlin.descriptors.commonizer.ResultsConsumer.ModuleResult
import org.jetbrains.kotlin.descriptors.commonizer.cir.*
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirClassFactory
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeFactory
import org.jetbrains.kotlin.descriptors.commonizer.konan.NativeSensitiveManifestData
import org.jetbrains.kotlin.descriptors.commonizer.konan.TargetedNativeManifestDataProvider
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.*
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

internal fun mockTAType(
    typeAliasId: String,
    nullable: Boolean = false,
    underlyingType: () -> CirClassOrTypeAliasType
): CirTypeAliasType = CirTypeFactory.createTypeAliasType(
    typeAliasId = createValidClassifierId(typeAliasId),
    underlyingType = CirTypeFactory.makeNullableIfNecessary(underlyingType(), nullable),
    arguments = emptyList(),
    isMarkedNullable = nullable
)

internal fun mockClassType(
    classId: String,
    nullable: Boolean = false
): CirClassType = CirTypeFactory.createClassType(
    classId = createValidClassifierId(classId),
    outerType = null,
    visibility = DescriptorVisibilities.PUBLIC,
    arguments = emptyList(),
    isMarkedNullable = nullable
)

private fun createValidClassifierId(classifierId: String): CirEntityId {
    check(classifierId.none { it == '.' || it == '\\' || it == '?' }) { "Malformed classifier ID: $classifierId" }
    return CirEntityId.create(classifierId)
}

internal val MOCK_CLASSIFIERS = CirKnownClassifiers(
    commonizedNodes = object : CirCommonizedClassifierNodes {
        private val MOCK_CLASS_NODE = CirClassNode(
            CommonizedGroup(0),
            LockBasedStorageManager.NO_LOCKS.createNullableLazyValue {
                CirClassFactory.create(
                    annotations = emptyList(),
                    name = CirName.create("Any"),
                    typeParameters = emptyList(),
                    visibility = DescriptorVisibilities.PUBLIC,
                    modality = Modality.OPEN,
                    kind = ClassKind.CLASS,
                    companion = null,
                    isCompanion = false,
                    isData = false,
                    isInline = false,
                    isInner = false,
                    isExternal = false
                )
            }
        )

        override fun classNode(classId: CirEntityId) = MOCK_CLASS_NODE
        override fun typeAliasNode(typeAliasId: CirEntityId) = error("This method should not be called")
        override fun addClassNode(classId: CirEntityId, node: CirClassNode) = error("This method should not be called")
        override fun addTypeAliasNode(typeAliasId: CirEntityId, node: CirTypeAliasNode) = error("This method should not be called")
    },
    forwardDeclarations = object : CirForwardDeclarations {
        override fun isExportedForwardDeclaration(classId: CirEntityId) = false
        override fun addExportedForwardDeclaration(classId: CirEntityId) = error("This method should not be called")
    },
    commonDependencies = CirProvidedClassifiers.EMPTY
)

internal class MockModulesProvider private constructor(
    private val modules: Map<String, ModuleDescriptor>,
) : ModulesProvider {
    private val moduleInfos = modules.keys.map { name -> fakeModuleInfo(name) }

    override fun loadModuleInfos() = moduleInfos

    override fun loadModuleMetadata(name: String): SerializedMetadata {
        val module = modules[name] ?: error("No such module: $name")
        return SERIALIZER.serializeModule(module)
    }

    private fun fakeModuleInfo(name: String) = ModuleInfo(name, File("/tmp/commonizer/mocks/$name"), null)

    companion object {
        @JvmName("createByModuleNames")
        fun create(moduleNames: List<String>) = MockModulesProvider(
            moduleNames.associateWith { name ->
                // expected special name for module
                val module = KotlinTestUtils.createEmptyModule("<$name>")
                module.initialize(PackageFragmentProvider.Empty)
                module.setDependencies(module)
                module
            }
        )

        @JvmName("createByModules")
        fun create(modules: List<ModuleDescriptor>) = MockModulesProvider(
            modules.associateBy { module -> module.name.strip() }
        )

        @JvmName("createBySingleModule")
        fun create(module: ModuleDescriptor) = MockModulesProvider(
            mapOf(module.name.strip() to module)
        )

        val SERIALIZER = KlibMetadataMonolithicSerializer(
            languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
            metadataVersion = KlibMetadataVersion.INSTANCE,
            skipExpects = false,
            project = null,
            includeOnlyModuleContent = true,
            allowErrorTypes = false
        )
    }
}

private typealias ModuleName = String
private typealias ModuleResults = HashMap<ModuleName, ModuleResult>

internal class MockResultsConsumer : ResultsConsumer {
    private val _modulesByTargets = LinkedHashMap<CommonizerTarget, ModuleResults>() // use linked hash map to preserve order
    val modulesByTargets: Map<CommonizerTarget, Collection<ModuleResult>>
        get() = _modulesByTargets.mapValues { it.value.values }

    val sharedTarget: SharedCommonizerTarget by lazy { modulesByTargets.keys.filterIsInstance<SharedCommonizerTarget>().single() }
    val leafTargets: Set<LeafCommonizerTarget> by lazy { modulesByTargets.keys.filterIsInstance<LeafCommonizerTarget>().toSet() }

    private val finishedTargets = mutableSetOf<CommonizerTarget>()

    lateinit var status: ResultsConsumer.Status

    override fun consume(target: CommonizerTarget, moduleResult: ModuleResult) {
        check(!this::status.isInitialized)
        check(target !in finishedTargets)
        val moduleResults: ModuleResults = _modulesByTargets.getOrPut(target) { ModuleResults() }
        val oldResult = moduleResults.put(moduleResult.libraryName, moduleResult)
        check(oldResult == null) // to avoid accidental overwriting
    }

    override fun targetConsumed(target: CommonizerTarget) {
        check(!this::status.isInitialized)
        check(target in _modulesByTargets.keys)
        check(target !in finishedTargets)
        finishedTargets += target
    }

    override fun allConsumed(status: ResultsConsumer.Status) {
        check(!this::status.isInitialized)
        check(finishedTargets.containsAll(_modulesByTargets.keys))
        this.status = status
    }
}

fun MockNativeManifestDataProvider(
    uniqueName: String = "mock",
    versions: KotlinLibraryVersioning = KotlinLibraryVersioning(null, null, null, null, null),
    dependencies: List<String> = emptyList(),
    isInterop: Boolean = true,
    packageFqName: String? = "mock",
    exportForwardDeclarations: List<String> = emptyList(),
    nativeTargets: Collection<String> = emptyList(),
    shortName: String? = "mock"
): TargetedNativeManifestDataProvider = TargetedNativeManifestDataProvider { _, _ ->
    NativeSensitiveManifestData(
        uniqueName = uniqueName,
        versions = versions,
        dependencies = dependencies,
        isInterop = isInterop,
        packageFqName = packageFqName,
        exportForwardDeclarations = exportForwardDeclarations,
        nativeTargets = nativeTargets,
        shortName = shortName
    )
}
