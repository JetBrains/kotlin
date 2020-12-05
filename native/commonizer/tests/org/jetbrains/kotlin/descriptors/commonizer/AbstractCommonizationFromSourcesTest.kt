/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import com.intellij.openapi.Disposable
import com.intellij.testFramework.PlatformTestUtil.lowercaseFirstLetter
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.common.CommonDependenciesContainer
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.commonizer.SourceModuleRoot.Companion.SHARED_TARGET_NAME
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.ClassCollector
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.FunctionCollector
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.collectMembers
import org.jetbrains.kotlin.descriptors.commonizer.mergedtree.collectNonEmptyPackageMemberScopes
import org.jetbrains.kotlin.descriptors.commonizer.utils.*
import org.jetbrains.kotlin.descriptors.commonizer.utils.MockBuiltInsProvider
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.test.KotlinTestUtils.*
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File
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

        return File(getHomeDirectory())
            .resolve("native/commonizer/testData")
            .resolve(testCaseDir)
            .resolve(testDir)
            .also(::assertIsDirectory)
    }

    protected fun doTestSuccessfulCommonization() {
        val sourceModuleRoots: SourceModuleRoots = SourceModuleRoots.load(getTestDataDir())
        val analyzedModules: AnalyzedModules = AnalyzedModules.create(sourceModuleRoots, testRootDisposable)

        val result: Result = runCommonization(analyzedModules.toCommonizationParameters())
        assertCommonizationPerformed(result)

        val sharedTarget: SharedTarget = analyzedModules.sharedTarget
        assertEquals(sharedTarget, result.sharedTarget)

        val sharedModuleAsExpected: ModuleDescriptor = analyzedModules.commonizedModules.getValue(sharedTarget)
        val sharedModuleByCommonizer: ModuleDescriptor =
            (result.modulesByTargets.getValue(sharedTarget).single() as ModuleResult.Commonized).module

        assertValidModule(sharedModuleAsExpected)
        assertValidModule(sharedModuleByCommonizer)
        assertModulesAreEqual(sharedModuleAsExpected, sharedModuleByCommonizer, "\"$sharedTarget\" target")

        val leafTargets: Set<LeafTarget> = analyzedModules.leafTargets
        assertEquals(leafTargets, result.leafTargets)

        for (leafTarget in leafTargets) {
            val leafTargetModuleAsExpected: ModuleDescriptor = analyzedModules.commonizedModules.getValue(leafTarget)
            val leafTargetModuleByCommonizer: ModuleDescriptor =
                (result.modulesByTargets.getValue(leafTarget).single() as ModuleResult.Commonized).module

            assertValidModule(leafTargetModuleAsExpected)
            assertValidModule(leafTargetModuleByCommonizer)
            assertModulesAreEqual(leafTargetModuleAsExpected, leafTargetModuleByCommonizer, "\"$leafTarget\" target")
        }
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
    val originalRoots: Map<LeafTarget, SourceModuleRoot>,
    val commonizedRoots: Map<Target, SourceModuleRoot>,
    val dependeeRoots: Map<Target, SourceModuleRoot>
) {
    val leafTargets: Set<LeafTarget> = originalRoots.keys
    val sharedTarget: SharedTarget

    init {
        check(leafTargets.size >= 2)
        check(leafTargets.none { it.name == SHARED_TARGET_NAME })

        val sharedTargets = commonizedRoots.keys.filterIsInstance<SharedTarget>()
        check(sharedTargets.size == 1)

        sharedTarget = sharedTargets.single()
        check(sharedTarget.targets == leafTargets)

        val allTargets = leafTargets + sharedTarget
        check(commonizedRoots.keys == allTargets)
        check(allTargets.containsAll(dependeeRoots.keys))
    }

    companion object {
        fun load(dataDir: File): SourceModuleRoots = try {
            val originalRoots = listRoots(dataDir, ORIGINAL_ROOTS_DIR).mapKeys { LeafTarget(it.key) }

            val leafTargets = originalRoots.keys
            val sharedTarget = SharedTarget(leafTargets)

            fun getTarget(targetName: String): Target =
                if (targetName == SHARED_TARGET_NAME) sharedTarget else leafTargets.first { it.name == targetName }

            val commonizedRoots = listRoots(dataDir, COMMONIZED_ROOTS_DIR).mapKeys { getTarget(it.key) }
            val dependeeRoots = listRoots(dataDir, DEPENDEE_ROOTS_DIR).mapKeys { getTarget(it.key) }

            SourceModuleRoots(originalRoots, commonizedRoots, dependeeRoots)
        } catch (e: Exception) {
            fail("Source module misconfiguration in $dataDir", cause = e)
        }

        private const val ORIGINAL_ROOTS_DIR = "original"
        private const val COMMONIZED_ROOTS_DIR = "commonized"
        private const val DEPENDEE_ROOTS_DIR = "dependee"

        private fun listRoots(dataDir: File, rootsDirName: String): Map<String, SourceModuleRoot> =
            dataDir.resolve(rootsDirName).listFiles()?.toSet().orEmpty().map(SourceModuleRoot::load).associateBy { it.targetName }
    }
}

private class AnalyzedModuleDependencies(
    val regularDependencies: Map<Target, List<ModuleDescriptor>>,
    val expectByDependencies: List<ModuleDescriptor>
) {
    fun withExpectByDependency(dependency: ModuleDescriptor) =
        AnalyzedModuleDependencies(
            regularDependencies = regularDependencies,
            expectByDependencies = expectByDependencies + dependency
        )

    companion object {
        val EMPTY = AnalyzedModuleDependencies(emptyMap(), emptyList())
    }
}

private class AnalyzedModules(
    val originalModules: Map<Target, ModuleDescriptor>,
    val commonizedModules: Map<Target, ModuleDescriptor>,
    val dependeeModules: Map<Target, List<ModuleDescriptor>>
) {
    val leafTargets: Set<LeafTarget>
    val sharedTarget: SharedTarget

    init {
        originalModules.keys.let { targets ->
            check(targets.isNotEmpty())

            leafTargets = targets.filterIsInstance<LeafTarget>().toSet()
            check(targets.size == leafTargets.size)
        }

        sharedTarget = SharedTarget(leafTargets)
        val allTargets = leafTargets + sharedTarget

        check(commonizedModules.keys == allTargets)
        check(allTargets.containsAll(dependeeModules.keys))
    }

    fun toCommonizationParameters(): Parameters {
        val parameters = Parameters()

        leafTargets.forEach { leafTarget ->
            val originalModule = originalModules.getValue(leafTarget)

            parameters.addTarget(
                TargetProvider(
                    target = leafTarget,
                    builtInsClass = originalModule.builtIns::class.java,
                    builtInsProvider = MockBuiltInsProvider(originalModule.builtIns),
                    modulesProvider = MockModulesProvider.create(originalModule),
                    dependeeModulesProvider = dependeeModules[leafTarget]?.let(MockModulesProvider::create)
                )
            )
        }

        parameters.dependeeModulesProvider = dependeeModules[sharedTarget]?.let(MockModulesProvider::create)

        return parameters
    }

    companion object {
        fun create(
            sourceModuleRoots: SourceModuleRoots,
            parentDisposable: Disposable
        ): AnalyzedModules = with(sourceModuleRoots) {
            // phase 1: provide the modules that are the dependencies for "original" and "commonized" modules
            val (dependeeModules, dependencies) = createDependeeModules(sharedTarget, dependeeRoots, parentDisposable)

            // phase 2: build "original" and "commonized" modules
            val originalModules = createModules(sharedTarget, originalRoots, dependencies, parentDisposable)
            val commonizedModules = createModules(sharedTarget, commonizedRoots, dependencies, parentDisposable)

            return AnalyzedModules(originalModules, commonizedModules, dependeeModules)
        }

        private fun createDependeeModules(
            sharedTarget: SharedTarget,
            dependeeRoots: Map<out Target, SourceModuleRoot>,
            parentDisposable: Disposable
        ): Pair<Map<Target, List<ModuleDescriptor>>, AnalyzedModuleDependencies> {
            val customDependeeModules =
                createModules(sharedTarget, dependeeRoots, AnalyzedModuleDependencies.EMPTY, parentDisposable, isDependeeModule = true)

            val stdlibModule = DefaultBuiltIns.Instance.builtInsModule

            val dependeeModules = (sharedTarget.targets + sharedTarget).associateWith { target ->
                // prepend stdlib for each target explicitly, so that the commonizer can see symbols from the stdlib
                listOfNotNull(stdlibModule, customDependeeModules[target])
            }

            return dependeeModules to AnalyzedModuleDependencies(
                regularDependencies = dependeeModules,
                expectByDependencies = dependeeModules.getValue(sharedTarget).filter { module -> module !== stdlibModule }
            )
        }

        private fun createModules(
            sharedTarget: SharedTarget,
            moduleRoots: Map<out Target, SourceModuleRoot>,
            dependencies: AnalyzedModuleDependencies,
            parentDisposable: Disposable,
            isDependeeModule: Boolean = false
        ): Map<Target, ModuleDescriptor> {
            val result = mutableMapOf<Target, ModuleDescriptor>()

            var dependenciesForOthers = dependencies

            // first, process the common module
            moduleRoots[sharedTarget]?.let { moduleRoot ->
                val commonModule = createModule(sharedTarget, sharedTarget, moduleRoot, dependencies, parentDisposable, isDependeeModule)
                result[sharedTarget] = commonModule
                dependenciesForOthers = dependencies.withExpectByDependency(commonModule)
            }

            // then, all platform modules
            moduleRoots.filterKeys { it != sharedTarget }.forEach { (leafTarget, moduleRoot) ->
                result[leafTarget] =
                    createModule(sharedTarget, leafTarget, moduleRoot, dependenciesForOthers, parentDisposable, isDependeeModule)
            }

            return result
        }

        private fun createModule(
            sharedTarget: SharedTarget,
            currentTarget: Target,
            moduleRoot: SourceModuleRoot,
            dependencies: AnalyzedModuleDependencies,
            parentDisposable: Disposable,
            isDependeeModule: Boolean
        ): ModuleDescriptor {
            val moduleName: String = moduleRoot.location.parentFile.parentFile.name.let {
                if (isDependeeModule) "dependee-$it" else it
            }
            check(Name.isValidIdentifier(moduleName))

            val configuration: CompilerConfiguration = newConfiguration()
            configuration.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

            val environment: KotlinCoreEnvironment = KotlinCoreEnvironment.createForTests(
                parentDisposable = parentDisposable,
                initialConfiguration = configuration,
                extensionConfigs = EnvironmentConfigFiles.METADATA_CONFIG_FILES
            )

            val psiFactory = KtPsiFactory(environment.project)

            val psiFiles: List<KtFile> = moduleRoot.location.walkTopDown()
                .filter { it.isFile }
                .map { psiFactory.createFile(it.name, doLoadFile(it)) }
                .toList()

            val module = CommonResolverForModuleFactory.analyzeFiles(
                files = psiFiles,
                moduleName = Name.special("<$moduleName>"),
                dependOnBuiltIns = true,
                languageVersionSettings = environment.configuration.languageVersionSettings,
                targetPlatform = CommonPlatforms.defaultCommonPlatform,
                dependenciesContainer = DependenciesContainerImpl(sharedTarget, currentTarget, dependencies)
            ) { content ->
                environment.createPackagePartProvider(content.moduleContentScope)
            }.moduleDescriptor

            if (!isDependeeModule)
                module.accept(PatchingTestDescriptorVisitor, Unit)

            return module
        }
    }
}

private class DependenciesContainerImpl(
    sharedTarget: SharedTarget,
    currentTarget: Target,
    dependencies: AnalyzedModuleDependencies
) : CommonDependenciesContainer {
    private val moduleInfoToModule = mutableMapOf<ModuleInfo, ModuleDescriptor>()
    private val expectByModuleInfos = mutableListOf<ModuleInfo>()
    private val regularModuleInfos = mutableListOf<ModuleInfo>()

    init {
        if (currentTarget != sharedTarget) {
            dependencies.expectByDependencies.forEach { expectByDependency ->
                val moduleInfo = ModuleInfoImpl(expectByDependency, emptyList())
                moduleInfoToModule[moduleInfo] = expectByDependency
                expectByModuleInfos += moduleInfo
            }
        }

        dependencies.regularDependencies[currentTarget]?.forEach { regularDependency ->
            val moduleInfo = ModuleInfoImpl(regularDependency, expectByModuleInfos)
            moduleInfoToModule[moduleInfo] = regularDependency
            regularModuleInfos += moduleInfo
        }

        regularModuleInfos += expectByModuleInfos
    }

    private inner class ModuleInfoImpl(
        private val module: ModuleDescriptor,
        private val regularDependencies: List<ModuleInfo>
    ) : ModuleInfo {
        override val name get() = module.name

        override fun dependencies() = listOf(this) + regularDependencies
        override fun dependencyOnBuiltIns() = ModuleInfo.DependencyOnBuiltIns.LAST

        override val platform get() = CommonPlatforms.defaultCommonPlatform
        override val analyzerServices get() = CommonPlatformAnalyzerServices
    }

    override val moduleInfos: List<ModuleInfo> get() = regularModuleInfos
    override val friendModuleInfos: List<ModuleInfo> get() = emptyList()
    override val refinesModuleInfos: List<ModuleInfo> get() = expectByModuleInfos

    override fun moduleDescriptorForModuleInfo(moduleInfo: ModuleInfo) =
        moduleInfoToModule[moduleInfo] ?: error("Unknown module info $moduleInfo")

    override fun registerDependencyForAllModules(moduleInfo: ModuleInfo, descriptorForModule: ModuleDescriptorImpl) = Unit
    override fun packageFragmentProviderForModuleInfo(moduleInfo: ModuleInfo): PackageFragmentProvider? = null
}

private object PatchingTestDescriptorVisitor : DeclarationDescriptorVisitorEmptyBodies<Unit, Unit>() {
    override fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: Unit) {
        descriptor.collectNonEmptyPackageMemberScopes { _, memberScope ->
            visitMemberScope(memberScope)
        }
    }

    private fun visitMemberScope(memberScope: MemberScope) {
        memberScope.collectMembers(
            ClassCollector {
                it.constructors.forEach(::visitCallableMemberDescriptor)
                visitMemberScope(it.unsubstitutedMemberScope)
            },
            FunctionCollector {
                visitCallableMemberDescriptor(it)
            },
            {
                true // eat and ignore everything else
            }
        )
    }

    private fun visitCallableMemberDescriptor(callableDescriptor: CallableMemberDescriptor) {
        val comment = callableDescriptor.findPsi()?.text?.lineSequence()?.firstOrNull()?.takeIf { it.startsWith("//") } ?: return
        val (key, value) = comment.substringAfter("//").split('=', limit = 2).takeIf { it.size == 2 }?.map { it.trim() } ?: return

        when (key) {
            "hasStableParameterNames" -> {
                if (!value.toBoolean()) (callableDescriptor as FunctionDescriptorImpl).setHasStableParameterNames(false)
            }
            else -> {
                // more custom actions may be added here in the future
            }
        }
    }
}
