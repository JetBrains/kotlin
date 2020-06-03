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
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentProvider
import org.jetbrains.kotlin.descriptors.commonizer.SourceModuleRoot.Companion.COMMON_TARGET_NAME
import org.jetbrains.kotlin.descriptors.commonizer.utils.assertCommonizationPerformed
import org.jetbrains.kotlin.descriptors.commonizer.utils.assertIsDirectory
import org.jetbrains.kotlin.descriptors.commonizer.utils.assertModulesAreEqual
import org.jetbrains.kotlin.descriptors.commonizer.utils.assertValidModule
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.platform.TargetPlatform
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.resolve.PlatformDependentAnalyzerServices
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

        fun Collection<ModuleDescriptor>.eachModuleAsTarget() = mapIndexed { index, moduleDescriptor ->
            InputTarget("target_$index") to moduleDescriptor
        }.toMap().toCommonizationParameters()
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

        val commonTarget: OutputTarget = analyzedModules.commonizedCommonModule.target
        assertEquals(commonTarget, result.commonTarget)

        val commonModuleAsExpected: ModuleDescriptor = analyzedModules.commonizedCommonModule.module
        val commonModuleByCommonizer: ModuleDescriptor = result.modulesByTargets.getValue(commonTarget).single()

        assertValidModule(commonModuleAsExpected)
        assertValidModule(commonModuleByCommonizer)
        assertModulesAreEqual(commonModuleAsExpected, commonModuleByCommonizer, "\"$commonTarget\" target")

        val concreteTargets: Set<InputTarget> = analyzedModules.commonizedPlatformModules.keys
        assertEquals(concreteTargets, result.concreteTargets)

        for (target in concreteTargets) {
            val targetModuleAsExpected: ModuleDescriptor = analyzedModules.commonizedPlatformModules.getValue(target).module
            val targetModuleByCommonizer: ModuleDescriptor = result.modulesByTargets.getValue(target).single()

            assertValidModule(targetModuleAsExpected)
            assertValidModule(targetModuleByCommonizer)
            assertModulesAreEqual(targetModuleAsExpected, targetModuleByCommonizer, "\"$target\" target")
        }
    }
}

private data class SourceModuleRoot(
    val targetName: String,
    val root: File
) {
    init {
        assertIsDirectory(root)
    }

    companion object {
        fun load(directory: File): SourceModuleRoot = SourceModuleRoot(
            targetName = directory.name,
            root = directory
        )

        const val COMMON_TARGET_NAME = "common"
    }
}

private class SourceModuleRoots(
    val originalPlatformRoots: Map<String, SourceModuleRoot>,
    val commonizedPlatformRoots: Map<String, SourceModuleRoot>,
    val commonizedCommonRoot: SourceModuleRoot
) {
    init {
        check(originalPlatformRoots.isNotEmpty())
        check(COMMON_TARGET_NAME !in originalPlatformRoots)
        check(originalPlatformRoots.keys == commonizedPlatformRoots.keys)
        check(commonizedCommonRoot.targetName == COMMON_TARGET_NAME)
    }

    companion object {
        fun load(dataDir: File): SourceModuleRoots = try {
            val originalRoots = listRoots(dataDir, ORIGINAL_ROOTS_DIR)
            val commonizedRoots = listRoots(dataDir, COMMONIZED_ROOTS_DIR)

            SourceModuleRoots(
                originalPlatformRoots = originalRoots,
                commonizedPlatformRoots = commonizedRoots - COMMON_TARGET_NAME,
                commonizedCommonRoot = commonizedRoots.getValue(COMMON_TARGET_NAME)
            )
        } catch (e: Exception) {
            fail("Source module misconfiguration in $dataDir", cause = e)
        }

        private const val ORIGINAL_ROOTS_DIR = "original"
        private const val COMMONIZED_ROOTS_DIR = "commonized"

        private fun listRoots(dataDir: File, rootsDirName: String): Map<String, SourceModuleRoot> =
            dataDir.resolve(rootsDirName).listFiles()?.toSet().orEmpty().map(SourceModuleRoot::load).associateBy { it.targetName }
    }
}

private class AnalyzedModule<T : Target>(
    val target: T,
    val module: ModuleDescriptor
) {
    companion object {
        fun <T : Target> create(
            target: T,
            sourceModuleRoot: SourceModuleRoot,
            commonSourceModuleRoot: SourceModuleRoot? = null,
            parentDisposable: Disposable
        ): AnalyzedModule<T> {
            val moduleName: String = sourceModuleRoot.root.parentFile.parentFile.name
            check(Name.isValidIdentifier(moduleName))

            return AnalyzedModule(
                target = target,
                module = analyze(
                    moduleName = moduleName,
                    moduleRoot = sourceModuleRoot.root,
                    commonModuleRoot = commonSourceModuleRoot?.root,
                    parentDisposable = parentDisposable
                )
            )
        }

        private fun analyze(
            moduleName: String,
            moduleRoot: File,
            commonModuleRoot: File?,
            parentDisposable: Disposable
        ): ModuleDescriptor {
            val commonModule: ModuleDescriptor? = if (commonModuleRoot != null) {
                analyzeModule(
                    moduleName = "common" + moduleName.capitalize(),
                    moduleRoot = commonModuleRoot,
                    dependencyContainer = null, // common module does not have any specific dependencies
                    parentDisposable = parentDisposable
                )
            } else null

            val module: ModuleDescriptor = analyzeModule(
                moduleName = moduleName,
                moduleRoot = moduleRoot,
                dependencyContainer = commonModule?.let(::CommonizedCommonDependenciesContainer), // platform module has dependencies to common module
                parentDisposable = parentDisposable
            )

            if (commonModule != null) {
                check(commonModule in module.expectedByModules)
                check(commonModule in module.allDependencyModules)
            }

            return module
        }

        private fun analyzeModule(
            moduleName: String,
            moduleRoot: File,
            dependencyContainer: CommonDependenciesContainer?,
            parentDisposable: Disposable
        ): ModuleDescriptor {
            val configuration: CompilerConfiguration = newConfiguration()
            configuration.put(CommonConfigurationKeys.MODULE_NAME, moduleName)

            val environment: KotlinCoreEnvironment = KotlinCoreEnvironment.createForTests(
                parentDisposable = parentDisposable,
                initialConfiguration = configuration,
                extensionConfigs = EnvironmentConfigFiles.METADATA_CONFIG_FILES
            )

            val psiFactory = KtPsiFactory(environment.project)

            val psiFiles: List<KtFile> = moduleRoot.walkTopDown()
                .filter { it.isFile }
                .map { psiFactory.createFile(it.name, doLoadFile(it)) }
                .toList()

            return CommonResolverForModuleFactory.analyzeFiles(
                files = psiFiles,
                moduleName = Name.special("<$moduleName>"),
                dependOnBuiltIns = true,
                languageVersionSettings = environment.configuration.languageVersionSettings,
                targetPlatform = CommonPlatforms.defaultCommonPlatform,
                dependenciesContainer = dependencyContainer
            ) { content ->
                environment.createPackagePartProvider(content.moduleContentScope)
            }.moduleDescriptor
        }
    }
}

private class AnalyzedModules(
    val originalPlatformModules: Map<InputTarget, AnalyzedModule<InputTarget>>,
    val commonizedPlatformModules: Map<InputTarget, AnalyzedModule<InputTarget>>,
    val commonizedCommonModule: AnalyzedModule<OutputTarget>
) {
    init {
        check(originalPlatformModules.isNotEmpty())
        check(originalPlatformModules.keys == commonizedPlatformModules.keys)
    }

    fun toCommonizationParameters(): Parameters = originalPlatformModules.mapValues { it.value.module }.toCommonizationParameters()

    companion object {
        fun create(
            sourceModuleRoots: SourceModuleRoots,
            parentDisposable: Disposable
        ): AnalyzedModules {
            val originalPlatformModules: Map<InputTarget, AnalyzedModule<InputTarget>> = createInputTargetModules(
                sourceModuleRoots = sourceModuleRoots.originalPlatformRoots,
                parentDisposable = parentDisposable
            )

            val commonizedCommonModule: AnalyzedModule<OutputTarget> = AnalyzedModule.create(
                target = OutputTarget(originalPlatformModules.keys),
                sourceModuleRoot = sourceModuleRoots.commonizedCommonRoot,
                parentDisposable = parentDisposable
            )

            val commonizedPlatformModules: Map<InputTarget, AnalyzedModule<InputTarget>> = createInputTargetModules(
                sourceModuleRoots = sourceModuleRoots.commonizedPlatformRoots,
                commonSourceModuleRoot = sourceModuleRoots.commonizedCommonRoot,
                parentDisposable = parentDisposable
            )

            return AnalyzedModules(
                originalPlatformModules = originalPlatformModules,
                commonizedPlatformModules = commonizedPlatformModules,
                commonizedCommonModule = commonizedCommonModule
            )
        }

        private fun createInputTargetModules(
            sourceModuleRoots: Map<String, SourceModuleRoot>,
            commonSourceModuleRoot: SourceModuleRoot? = null,
            parentDisposable: Disposable
        ): Map<InputTarget, AnalyzedModule<InputTarget>> = sourceModuleRoots.map { (targetName, sourceModuleRoot) ->
            AnalyzedModule.create(
                target = InputTarget(targetName),
                sourceModuleRoot = sourceModuleRoot,
                commonSourceModuleRoot = commonSourceModuleRoot,
                parentDisposable = parentDisposable
            )
        }.associateBy { it.target }
    }
}

private class CommonizedCommonDependenciesContainer(
    private val commonModule: ModuleDescriptor
) : CommonDependenciesContainer {
    private val commonModuleInfo = object : ModuleInfo {
        override val name: Name get() = commonModule.name

        override fun dependencies(): List<ModuleInfo> = listOf(this)
        override fun dependencyOnBuiltIns(): ModuleInfo.DependencyOnBuiltIns = ModuleInfo.DependencyOnBuiltIns.LAST

        override val platform: TargetPlatform get() = CommonPlatforms.defaultCommonPlatform
        override val analyzerServices: PlatformDependentAnalyzerServices get() = CommonPlatformAnalyzerServices
    }

    override val moduleInfos: List<ModuleInfo> get() = listOf(commonModuleInfo)

    override fun moduleDescriptorForModuleInfo(moduleInfo: ModuleInfo): ModuleDescriptor {
        if (moduleInfo !== commonModuleInfo)
            error("Unknown module info $moduleInfo")

        return commonModule
    }

    override fun registerDependencyForAllModules(moduleInfo: ModuleInfo, descriptorForModule: ModuleDescriptorImpl) = Unit
    override fun packageFragmentProviderForModuleInfo(moduleInfo: ModuleInfo): PackageFragmentProvider? = null

    override val friendModuleInfos: List<ModuleInfo> get() = emptyList()
    override val refinesModuleInfos: List<ModuleInfo> get() = listOf(commonModuleInfo)
}

private fun Map<InputTarget, ModuleDescriptor>.toCommonizationParameters(): Parameters = Parameters().also {
    forEach { (target, moduleDescriptor) ->
        it.addTarget(
            TargetProvider(
                target = target,
                builtInsClass = moduleDescriptor.builtIns::class.java,
                builtInsProvider = BuiltInsProvider.wrap(moduleDescriptor.builtIns),
                modulesProvider = ModulesProvider.wrap(moduleDescriptor)
            )
        )
    }
}
