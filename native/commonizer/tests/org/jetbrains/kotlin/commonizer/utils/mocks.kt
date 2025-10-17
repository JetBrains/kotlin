/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("TestFunctionName")

package org.jetbrains.kotlin.commonizer.utils

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.commonizer.*
import org.jetbrains.kotlin.commonizer.ModulesProvider.ModuleInfo
import org.jetbrains.kotlin.commonizer.ResultsConsumer.ModuleResult
import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.konan.NativeManifestDataProvider
import org.jetbrains.kotlin.commonizer.konan.NativeSensitiveManifestData
import org.jetbrains.kotlin.commonizer.mergedtree.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.library.KotlinLibraryVersioning
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.storage.LockBasedStorageManager

internal fun mockTAType(
    typeAliasId: String,
    nullable: Boolean = false,
    underlyingType: () -> CirClassOrTypeAliasType
): CirTypeAliasType = CirTypeAliasType.createInterned(
    typeAliasId = createValidClassifierId(typeAliasId),
    underlyingType = underlyingType().makeNullableIfNecessary(nullable),
    arguments = emptyList(),
    isMarkedNullable = nullable
)

internal fun mockClassType(
    classId: String,
    nullable: Boolean = false
): CirClassType = CirClassType.createInterned(
    classId = createValidClassifierId(classId),
    outerType = null,
    arguments = emptyList(),
    isMarkedNullable = nullable
)

internal fun mockExtensionReceiver(receiverClassId: String) = CirExtensionReceiver(
    annotations = emptyList(),
    type = mockClassType(receiverClassId)
)

private fun createValidClassifierId(classifierId: String): CirEntityId {
    check(classifierId.none { it == '\\' || it == '?' }) { "Malformed classifier ID: $classifierId" }
    return CirEntityId.create(classifierId)
}

internal val MOCK_CLASSIFIERS = CirKnownClassifiers(
    classifierIndices = TargetDependent.empty(),
    targetDependencies = TargetDependent.empty(),
    commonizedNodes = object : CirCommonizedClassifierNodes {
        override fun classNode(classId: CirEntityId) = CirClassNode(
            classId,
            CommonizedGroup(0),
            LockBasedStorageManager.NO_LOCKS.createNullableLazyValue {
                CirClass.create(
                    annotations = emptyList(),
                    name = CirName.create("Any"),
                    typeParameters = emptyList(),
                    supertypes = emptyList(),
                    visibility = Visibilities.Public,
                    modality = Modality.OPEN,
                    kind = ClassKind.CLASS,
                    companion = null,
                    isCompanion = false,
                    isData = false,
                    isValue = false,
                    isInner = false,
                    hasEnumEntries = false
                )
            }
        )

        override fun typeAliasNode(typeAliasId: CirEntityId) = error("This method should not be called")
        override fun addClassNode(classId: CirEntityId, node: CirClassNode) = error("This method should not be called")
        override fun addTypeAliasNode(typeAliasId: CirEntityId, node: CirTypeAliasNode) = error("This method should not be called")
    },
    commonDependencies = CirProvidedClassifiers.EMPTY
)

internal class MockModulesProvider private constructor(
    private val modules: Map<String, SerializedMetadata>,
) : ModulesProvider {
    override val moduleInfos = modules.keys.map { name -> fakeModuleInfo(name) }

    override fun loadModuleMetadata(name: String): SerializedMetadata {
        val module = modules[name] ?: error("No such module: $name")
        return module
    }

    private fun fakeModuleInfo(name: String) = ModuleInfo(name, null)

    companion object {
        @JvmName("createByModuleNames")
        fun create(moduleNames: List<String>, disposable: Disposable) = MockModulesProvider(
            moduleNames.associateWith { name ->
                createEmptyModule(name, disposable)
            }
        )

        @JvmName("createByModules")
        fun create(modules: List<CompiledDependency>) = MockModulesProvider(
            modules.associateBy(
                keySelector = { (namedMetadata) -> namedMetadata.name.strip() },
                valueTransform = { (namedMetadata) -> namedMetadata.metadata }
            )
        )

        @JvmName("createByModulesWithNames")
        fun create(modules: List<NamedMetadata>) = MockModulesProvider(
            modules.associateBy(
                keySelector = { (name, _) -> name.strip() },
                valueTransform = { (_, module) -> module }
            )
        )

        @JvmName("createBySingleModule")
        fun create(module: CompiledDependency) = create(module.namedMetadata)

        @JvmName("createBySingleModule")
        fun create(nameToModule: NamedMetadata) = MockModulesProvider(
            mapOf(nameToModule.name.strip() to nameToModule.metadata)
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

    override fun consume(parameters: CommonizerParameters, target: CommonizerTarget, moduleResult: ModuleResult) {
        check(!this::status.isInitialized)
        check(target !in finishedTargets) { "$target already finished" }
        val moduleResults: ModuleResults = _modulesByTargets.getOrPut(target) { ModuleResults() }
        val oldResult = moduleResults.put(moduleResult.libraryName, moduleResult)
        check(oldResult == null) // to avoid accidental overwriting
    }

    override fun targetConsumed(parameters: CommonizerParameters, target: CommonizerTarget) {
        check(!this::status.isInitialized)
        check(target !in finishedTargets)
        finishedTargets += target
    }

    override fun allConsumed(parameters: CommonizerParameters, status: ResultsConsumer.Status) {
        check(!this::status.isInitialized)
        check(finishedTargets.containsAll(_modulesByTargets.keys))
        this.status = status
    }
}

fun MockNativeManifestDataProvider(
    target: CommonizerTarget,
    uniqueName: String = "mock",
    versions: KotlinLibraryVersioning = KotlinLibraryVersioning(null, null, null),
    dependencies: List<String> = emptyList(),
    isCInterop: Boolean = true,
    packageFqName: String? = "mock",
    exportForwardDeclarations: List<String> = emptyList(),
    includedForwardDeclarations: List<String> = emptyList(),
    nativeTargets: Collection<String> = emptyList(),
    shortName: String? = "mock"
): NativeManifestDataProvider = object : NativeManifestDataProvider {
    override fun buildManifest(libraryName: UniqueLibraryName): NativeSensitiveManifestData {
        return NativeSensitiveManifestData(
            uniqueName = uniqueName,
            versions = versions,
            dependencies = dependencies,
            isCInterop = isCInterop,
            packageFqName = packageFqName,
            exportForwardDeclarations = exportForwardDeclarations,
            includedForwardDeclarations = includedForwardDeclarations,
            nativeTargets = nativeTargets,
            shortName = shortName,
            commonizerTarget = target,
        )
    }
}
