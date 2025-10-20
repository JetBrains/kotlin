/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.common.metadataDestinationDirectory
import org.jetbrains.kotlin.cli.jvm.config.JvmClasspathRoot
import org.jetbrains.kotlin.cli.pipeline.PipelinePhase
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataFrontendPipelineArtifact
import org.jetbrains.kotlin.cli.pipeline.metadata.MetadataKlibFileWriterPhase
import org.jetbrains.kotlin.commonizer.ResultsConsumer.Status
import org.jetbrains.kotlin.commonizer.SourceModuleRoot.Companion.SHARED_TARGET_NAME
import org.jetbrains.kotlin.commonizer.konan.NativeManifestDataProvider
import org.jetbrains.kotlin.commonizer.utils.*
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.copy
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.psi
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import java.io.File
import kotlin.collections.map
import kotlin.collections.orEmpty
import kotlin.collections.plus
import kotlin.contracts.ExperimentalContracts
import kotlin.test.fail

@ExperimentalContracts
abstract class AbstractCommonizationFromSourcesTest : KtUsefulTestCase() {
    companion object {
        init {
            System.setProperty("java.awt.headless", "true")
        }
    }

    private fun getTestDataDir(): File {
        val testCaseDir = lowercaseFirstLetter(
            this::class.java.simpleName.substringBefore("FromSources").substringBefore("Test"),
            true
        )
        val testDir = testDirectoryName

        return File(KtTestUtil.getHomeDirectory())
            .resolve("native/commonizer/testData")
            .resolve(testCaseDir)
            .resolve(testDir)
            .also(::assertIsDirectory)
    }

    protected fun doTestSuccessfulCommonization() {
        val sourceModuleRoots: SourceModuleRoots = SourceModuleRoots.load(getTestDataDir())
        val analyzedModules: AnalyzedModules = AnalyzedModules.create(sourceModuleRoots, testRootDisposable)

        val results = MockResultsConsumer()
        runCommonization(analyzedModules.toCommonizerParameters(results))
        assertEquals(Status.DONE, results.status)

        val sharedTarget: SharedCommonizerTarget = analyzedModules.sharedTarget
        assertEquals(sharedTarget, results.sharedTarget)

        val sharedModuleAsExpected: SerializedMetadata = analyzedModules.commonizedModules.getValue(sharedTarget)
        val sharedModuleByCommonizer: SerializedMetadata = results.modulesByTargets.getValue(sharedTarget).single().metadata

        assertModulesAreEqual(sharedModuleAsExpected, sharedModuleByCommonizer, sharedTarget)
    }
}

private data class SourceModuleRoot(
    val targetName: String,
    val location: File
) {
    init {
        assertIsDirectory(location)
    }

    companion object {
        fun load(directory: File): SourceModuleRoot = SourceModuleRoot(
            targetName = directory.name,
            location = directory
        )

        const val SHARED_TARGET_NAME = "common"
    }
}

private class SourceModuleRoots(
    val originalRoots: Map<LeafCommonizerTarget, SourceModuleRoot>,
    val commonizedRoots: Map<CommonizerTarget, SourceModuleRoot>,
    val dependencyRoots: Map<CommonizerTarget, SourceModuleRoot>
) {
    val leafTargets: Set<LeafCommonizerTarget> = originalRoots.keys
    val sharedTarget: SharedCommonizerTarget

    init {
        check(leafTargets.size >= 2)
        check(leafTargets.none { it.name == SHARED_TARGET_NAME })

        val sharedTargets = commonizedRoots.keys.filterIsInstance<SharedCommonizerTarget>()
        check(sharedTargets.size == 1)

        sharedTarget = sharedTargets.single()
        check(sharedTarget.targets == leafTargets)

        val allTargets = leafTargets + sharedTarget
        check(commonizedRoots.keys.single() == sharedTarget)
        check(allTargets.containsAll(dependencyRoots.keys))
    }

    companion object {
        fun load(dataDir: File): SourceModuleRoots = try {
            val originalRoots = listRoots(dataDir, ORIGINAL_ROOTS_DIR).mapKeys { LeafCommonizerTarget(it.key) }

            val leafTargets = originalRoots.keys
            val sharedTarget = SharedCommonizerTarget(leafTargets)

            fun getTarget(targetName: String): CommonizerTarget =
                if (targetName == SHARED_TARGET_NAME) sharedTarget else leafTargets.first { it.name == targetName }

            val commonizedRoots = listRoots(dataDir, COMMONIZED_ROOTS_DIR).mapKeys { getTarget(it.key) }
            val dependencyRoots = listRoots(dataDir, DEPENDENCY_ROOTS_DIR).mapKeys { getTarget(it.key) }

            SourceModuleRoots(originalRoots, commonizedRoots, dependencyRoots)
        } catch (e: Exception) {
            fail("Source module misconfiguration in $dataDir", cause = e)
        }

        private const val ORIGINAL_ROOTS_DIR = "original"
        private const val COMMONIZED_ROOTS_DIR = "commonized"
        private const val DEPENDENCY_ROOTS_DIR = "dependency"

        private fun listRoots(dataDir: File, rootsDirName: String): Map<String, SourceModuleRoot> =
            dataDir.resolve(rootsDirName).listFiles()?.toSet().orEmpty().map(SourceModuleRoot::load).associateBy { it.targetName }
    }
}

data class CompiledDependency(
    val namedMetadata: NamedMetadata,
    val destination: String,
)

private class AnalyzedModuleDependencies(
    val regularDependencies: Map<CommonizerTarget, List<CompiledDependency>>,
    val expectByDependencies: List<CompiledDependency>
) {
    fun withExpectByDependency(dependency: CompiledDependency) =
        AnalyzedModuleDependencies(
            regularDependencies = regularDependencies,
            expectByDependencies = expectByDependencies + dependency
        )

    companion object {
        val EMPTY = AnalyzedModuleDependencies(emptyMap(), emptyList())
    }
}

private class AnalyzedModules(
    val originalModules: Map<CommonizerTarget, CompiledDependency>,
    val commonizedModules: Map<CommonizerTarget, SerializedMetadata>,
    val dependencyModules: Map<CommonizerTarget, List<CompiledDependency>>
) {
    val leafTargets: Set<LeafCommonizerTarget>
    val sharedTarget: SharedCommonizerTarget

    init {
        originalModules.keys.let { targets ->
            check(targets.isNotEmpty())

            leafTargets = targets.filterIsInstance<LeafCommonizerTarget>().toSet()
            check(targets.size == leafTargets.size)
        }

        sharedTarget = SharedCommonizerTarget(leafTargets)
        val allTargets = leafTargets + sharedTarget

        check(commonizedModules.keys.single() == sharedTarget)
        check(allTargets.containsAll(dependencyModules.keys))
    }

    fun toCommonizerParameters(
        resultsConsumer: ResultsConsumer,
        manifestDataProvider: (CommonizerTarget) -> NativeManifestDataProvider = { MockNativeManifestDataProvider(it) },
        commonizerSettings: CommonizerSettings = DefaultCommonizerSettings,
    ) = CommonizerParameters(
        outputTargets = setOf(SharedCommonizerTarget(leafTargets.toSet())),
        manifestProvider = TargetDependent(sharedTarget.withAllLeaves(), manifestDataProvider),
        dependenciesProvider = TargetDependent(sharedTarget.withAllLeaves()) { target ->
            dependencyModules
                .filter { (registeredTarget, _) -> target in registeredTarget.withAllLeaves() }
                .values.flatten()
                .map { it.namedMetadata }
                .plus(loadStdlibMetadata())
                .let(MockModulesProvider::create)
        },
        targetProviders = TargetDependent(leafTargets) { leafTarget ->
            TargetProvider(
                target = leafTarget,
                modulesProvider = MockModulesProvider.create(originalModules.getValue(leafTarget))
            )
        },
        resultsConsumer = resultsConsumer,
        settings = commonizerSettings,
    )

    companion object {
        fun create(
            sourceModuleRoots: SourceModuleRoots,
            parentDisposable: Disposable
        ): AnalyzedModules = with(sourceModuleRoots) {
            // phase 1: provide the modules that are the dependencies for "original" and "commonized" modules
            val (dependencyModules: Map<CommonizerTarget, List<CompiledDependency>>, dependencies: AnalyzedModuleDependencies) =
                createDependencyModules(sharedTarget, dependencyRoots, parentDisposable)

            // phase 2: build "original" and "commonized" modules
            val originalModules: Map<CommonizerTarget, CompiledDependency> =
                createModules(sharedTarget, originalRoots, dependencies, parentDisposable)

            val commonizedModules: Map<CommonizerTarget, SerializedMetadata> =
                createModules(sharedTarget, commonizedRoots, dependencies, parentDisposable)
                    .mapValues { (_, dependency) -> dependency.namedMetadata.metadata }

            return AnalyzedModules(originalModules, commonizedModules, dependencyModules)
        }

        private fun createDependencyModules(
            sharedTarget: SharedCommonizerTarget,
            dependencyRoots: Map<out CommonizerTarget, SourceModuleRoot>,
            parentDisposable: Disposable
        ): Pair<Map<CommonizerTarget, List<CompiledDependency>>, AnalyzedModuleDependencies> {
            val customDependencyModules =
                createModules(sharedTarget, dependencyRoots, AnalyzedModuleDependencies.EMPTY, parentDisposable, isDependencyModule = true)

            val dependencyModules = (sharedTarget.targets + sharedTarget).associateWith { target ->
                listOfNotNull(customDependencyModules[target])
            }

            return dependencyModules to AnalyzedModuleDependencies(
                regularDependencies = dependencyModules,
                expectByDependencies = dependencyModules.getValue(sharedTarget)
            )
        }

        private fun createModules(
            sharedTarget: SharedCommonizerTarget,
            moduleRoots: Map<out CommonizerTarget, SourceModuleRoot>,
            dependencies: AnalyzedModuleDependencies,
            parentDisposable: Disposable,
            isDependencyModule: Boolean = false
        ): Map<CommonizerTarget, CompiledDependency> {
            val result = mutableMapOf<CommonizerTarget, CompiledDependency>()

            var dependenciesForOthers = dependencies

            // first, process the common module
            moduleRoots[sharedTarget]?.let { moduleRoot ->
                val commonModule = createModule(sharedTarget, sharedTarget, moduleRoot, dependencies, parentDisposable, isDependencyModule)
                result[sharedTarget] = commonModule
                dependenciesForOthers = dependencies.withExpectByDependency(commonModule)
            }

            // then, all platform modules
            moduleRoots.filterKeys { it != sharedTarget }.forEach { (leafTarget, moduleRoot) ->
                result[leafTarget] = createModule(
                    sharedTarget, leafTarget, moduleRoot,
                    dependenciesForOthers, parentDisposable, isDependencyModule
                )
            }

            return result
        }

        private fun createModule(
            sharedTarget: SharedCommonizerTarget,
            currentTarget: CommonizerTarget,
            moduleRoot: SourceModuleRoot,
            dependencies: AnalyzedModuleDependencies,
            parentDisposable: Disposable,
            isDependencyModule: Boolean
        ): CompiledDependency {
            val moduleName: String = moduleRoot.location.parentFile.parentFile.name.let {
                if (isDependencyModule) "dependency-$it" else it
            }

            val (configuration, serializationArtifact) = serializeModuleToMetadata(
                moduleName, moduleRoot.location,
                disposable = parentDisposable,
                isCommon = true,
                regularDependencies = dependencies.regularDependencies[currentTarget].orEmpty().map {
                    JvmClasspathRoot(File(it.destination))
                },
                refinesDependencies = when {
                    currentTarget != sharedTarget -> dependencies.expectByDependencies.map { it.destination }
                    else -> emptyList()
                },
                firTransformationPhase = TestPatchingPipelinePhase.takeIf { !isDependencyModule },
            )

            val compiledDependenciesRoot = FileUtil.createTempDirectory(moduleName, null)

            configuration.metadataDestinationDirectory = File(compiledDependenciesRoot.path + File.pathSeparator + moduleName)
            val destination = MetadataKlibFileWriterPhase.executePhase(serializationArtifact).destination

            return CompiledDependency(serializationArtifact.metadata named moduleName, destination)
        }
    }
}

/**
 * Modifies FIR trees in-place according to additional directive in test files.
 */
object TestPatchingPipelinePhase : PipelinePhase<MetadataFrontendPipelineArtifact, MetadataFrontendPipelineArtifact>(
    name = "TestPatchingPipelinePhase",
) {
    override fun executePhase(input: MetadataFrontendPipelineArtifact) = input.also {
        for (output in input.result.outputs) {
            for (firFile in output.fir) {
                firFile.accept(TestPatchingFirVisitor)
            }
        }
    }
}

private object TestPatchingFirVisitor : FirVisitorVoid() {
    override fun visitElement(element: FirElement) {
        element.acceptChildren(this)
    }

    override fun visitNamedFunction(namedFunction: FirNamedFunction) {
        val comment = namedFunction.source.psi?.text?.lineSequence()?.firstOrNull()?.takeIf { it.startsWith("//") }
            ?: return
        val (key, value) = comment.substringAfter("//").split('=', limit = 2).takeIf { it.size == 2 }?.map { it.trim() }
            ?: return

        when (key) {
            "hasStableParameterNames" -> when {
                !value.toBoolean() -> namedFunction.replaceStatus(namedFunction.status.copy(hasStableParameterNames = false))
            }
            else -> {
                // more custom actions may be added here in the future
            }
        }

        namedFunction.acceptChildren(this)
    }
}
