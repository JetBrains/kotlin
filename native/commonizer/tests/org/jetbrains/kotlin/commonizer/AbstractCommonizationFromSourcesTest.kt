/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer

import com.intellij.openapi.Disposable
import org.jetbrains.kotlin.analyzer.ModuleInfo
import org.jetbrains.kotlin.analyzer.common.CommonDependenciesContainer
import org.jetbrains.kotlin.analyzer.common.CommonPlatformAnalyzerServices
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.commonizer.ResultsConsumer.ModuleResult
import org.jetbrains.kotlin.commonizer.ResultsConsumer.Status
import org.jetbrains.kotlin.commonizer.SourceModuleRoot.Companion.SHARED_TARGET_NAME
import org.jetbrains.kotlin.commonizer.konan.NativeManifestDataProvider
import org.jetbrains.kotlin.commonizer.utils.*
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.library.SerializedMetadata
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.CompilerEnvironment
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.test.KotlinTestUtils.newConfiguration
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.utils.alwaysTrue
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

private class AnalyzedModuleDependencies(
    val regularDependencies: Map<CommonizerTarget, List<ModuleDescriptor>>,
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
    val originalModules: Map<CommonizerTarget, ModuleDescriptor>,
    val commonizedModules: Map<CommonizerTarget, SerializedMetadata>,
    val dependencyModules: Map<CommonizerTarget, List<ModuleDescriptor>>
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
            val (dependencyModules: Map<CommonizerTarget, List<ModuleDescriptor>>, dependencies: AnalyzedModuleDependencies) =
                createDependencyModules(sharedTarget, dependencyRoots, parentDisposable)

            // phase 2: build "original" and "commonized" modules
            val originalModules: Map<CommonizerTarget, ModuleDescriptor> =
                createModules(sharedTarget, originalRoots, dependencies, parentDisposable)

            val commonizedModules: Map<CommonizerTarget, SerializedMetadata> =
                createModules(sharedTarget, commonizedRoots, dependencies, parentDisposable)
                    .mapValues { (_, moduleDescriptor) -> MockModulesProvider.SERIALIZER.serializeModule(moduleDescriptor) }

            return AnalyzedModules(originalModules, commonizedModules, dependencyModules)
        }

        private fun createDependencyModules(
            sharedTarget: SharedCommonizerTarget,
            dependencyRoots: Map<out CommonizerTarget, SourceModuleRoot>,
            parentDisposable: Disposable
        ): Pair<Map<CommonizerTarget, List<ModuleDescriptor>>, AnalyzedModuleDependencies> {
            val customDependencyModules =
                createModules(sharedTarget, dependencyRoots, AnalyzedModuleDependencies.EMPTY, parentDisposable, isDependencyModule = true)

            val stdlibModule = DefaultBuiltIns.Instance.builtInsModule

            val dependencyModules = (sharedTarget.targets + sharedTarget).associateWith { target ->
                // prepend stdlib for each target explicitly, so that the commonizer can see symbols from the stdlib
                listOfNotNull(stdlibModule, customDependencyModules[target])
            }

            return dependencyModules to AnalyzedModuleDependencies(
                regularDependencies = dependencyModules,
                expectByDependencies = dependencyModules.getValue(sharedTarget).filter { module -> module !== stdlibModule }
            )
        }

        private fun createModules(
            sharedTarget: SharedCommonizerTarget,
            moduleRoots: Map<out CommonizerTarget, SourceModuleRoot>,
            dependencies: AnalyzedModuleDependencies,
            parentDisposable: Disposable,
            isDependencyModule: Boolean = false
        ): Map<CommonizerTarget, ModuleDescriptor> {
            val result = mutableMapOf<CommonizerTarget, ModuleDescriptor>()

            var dependenciesForOthers = dependencies

            // first, process the common module
            moduleRoots[sharedTarget]?.let { moduleRoot ->
                val commonModule = createModule(sharedTarget, sharedTarget, moduleRoot, dependencies, parentDisposable, isDependencyModule)
                result[sharedTarget] = commonModule
                dependenciesForOthers = dependencies.withExpectByDependency(commonModule)
            }

            // then, all platform modules
            moduleRoots.filterKeys { it != sharedTarget }.forEach { (leafTarget, moduleRoot) ->
                result[leafTarget] =
                    createModule(sharedTarget, leafTarget, moduleRoot, dependenciesForOthers, parentDisposable, isDependencyModule)
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
        ): ModuleDescriptor {
            val moduleName: String = moduleRoot.location.parentFile.parentFile.name.let {
                if (isDependencyModule) "dependency-$it" else it
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
                .map { psiFactory.createFile(it.name, KtTestUtil.doLoadFile(it)) }
                .toList()

            val module = CommonResolverForModuleFactory.analyzeFiles(
                psiFiles,
                Name.special("<$moduleName>"),
                dependOnBuiltIns = true,
                environment.configuration.languageVersionSettings,
                CommonPlatforms.defaultCommonPlatform,
                CompilerEnvironment,
                dependenciesContainer = DependenciesContainerImpl(sharedTarget, currentTarget, dependencies),
            ) { content ->
                environment.createPackagePartProvider(content.moduleContentScope)
            }.moduleDescriptor

            if (!isDependencyModule)
                module.accept(PatchingTestDescriptorVisitor, Unit)

            return module
        }
    }
}

private class DependenciesContainerImpl(
    sharedTarget: SharedCommonizerTarget,
    currentTarget: CommonizerTarget,
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
        // we don's need to process fragments from other modules which are the dependencies of this module, so
        // let's use the appropriate package fragment provider
        val packageFragmentProvider = (descriptor as ModuleDescriptorImpl).packageFragmentProviderForModuleContentWithoutDependencies

        fun recurse(packageFqName: FqName) {
            val ownPackageMemberScopes = packageFragmentProvider.packageFragments(packageFqName)
                .asSequence()
                .map { it.getMemberScope() }
                .filter { it != MemberScope.Empty }
                .toList()

            if (ownPackageMemberScopes.isNotEmpty()) {
                // don't include subpackages into chained member scope
                val memberScope = ChainedMemberScope.create(
                    "package member scope for $packageFqName",
                    ownPackageMemberScopes
                )

                visitMemberScope(memberScope)
            }

            packageFragmentProvider.getSubPackagesOf(packageFqName, alwaysTrue()).toSet().map { recurse(it) }
        }

        recurse(FqName.ROOT)
    }

    private fun visitMemberScope(memberScope: MemberScope) {
        memberScope.getContributedDescriptors().forEach { descriptor ->
            when (descriptor) {
                is ClassDescriptor -> {
                    descriptor.constructors.forEach(::visitCallableMemberDescriptor)
                    visitMemberScope(descriptor.unsubstitutedMemberScope)
                }
                is SimpleFunctionDescriptor -> {
                    if (descriptor.kind.isReal && !descriptor.isKniBridgeFunction() && !descriptor.isDeprecatedTopLevelFunction()) {
                        visitCallableMemberDescriptor(descriptor)
                    }
                }
                else -> Unit // ignore everything else
            }
        }
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

    private fun SimpleFunctionDescriptor.isKniBridgeFunction() =
        name.asString().startsWith(KNI_BRIDGE_FUNCTION_PREFIX)

    private fun SimpleFunctionDescriptor.isDeprecatedTopLevelFunction() =
        containingDeclaration is PackageFragmentDescriptor && annotations.hasAnnotation(DEPRECATED_ANNOTATION_FQN)
}
