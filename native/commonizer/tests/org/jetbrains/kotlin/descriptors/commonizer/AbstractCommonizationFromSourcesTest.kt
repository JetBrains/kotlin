/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer

import com.intellij.testFramework.PlatformTestUtil.lowercaseFirstLetter
import org.jetbrains.kotlin.analyzer.common.CommonResolverForModuleFactory
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.config.CommonConfigurationKeys
import org.jetbrains.kotlin.config.languageVersionSettings
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.commonizer.utils.assertCommonizationPerformed
import org.jetbrains.kotlin.descriptors.commonizer.utils.assertIsDirectory
import org.jetbrains.kotlin.descriptors.commonizer.utils.assertModulesAreEqual
import org.jetbrains.kotlin.descriptors.commonizer.utils.assertValidModule
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.platform.CommonPlatforms
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.KotlinTestUtils.*
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File
import kotlin.contracts.ExperimentalContracts

@ExperimentalContracts
@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractCommonizationFromSourcesTest : KtUsefulTestCase() {
    companion object {
        init {
            System.setProperty("java.awt.headless", "true")
        }

        fun Collection<ModuleDescriptor>.eachModuleAsTarget() = mapIndexed { index, moduleDescriptor ->
            InputTarget("target_$index") to moduleDescriptor
        }.toMap().toCommonizationParameters()

        fun Map<InputTarget, ModuleDescriptor>.toCommonizationParameters() = Parameters().also {
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
    }

    protected fun createEnvironment(bareModuleName: String): KotlinCoreEnvironment {
        check(Name.isValidIdentifier(bareModuleName))
        val configuration = newConfiguration()
        configuration.put(CommonConfigurationKeys.MODULE_NAME, bareModuleName)

        return KotlinCoreEnvironment.createForTests(testRootDisposable, configuration, EnvironmentConfigFiles.METADATA_CONFIG_FILES)
    }

    protected val KotlinCoreEnvironment.moduleName: Name
        get() {
            val bareModuleName = configuration.getNotNull(CommonConfigurationKeys.MODULE_NAME)
            return Name.special("<$bareModuleName>")
        }

    protected val testDataDir: File
        get() {
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

    protected val sourceModuleRoots: Triple<Set<File>, Set<File>, File>
        get() {
            val testDataDir = testDataDir

            val originalRoots = testDataDir
                .resolve("original")
                .also(::assertIsDirectory)
                .listFiles()
                ?.toSet()
                ?.also { it.forEach(::assertIsDirectory) }

            val commonizedRoots = testDataDir
                .resolve("commonized")
                .also(::assertIsDirectory)
                .listFiles()
                ?.toSet()
                ?.also { it.forEach(::assertIsDirectory) }

            val commonRoot = commonizedRoots?.singleOrNull { it.name == "common" }

            check(
                !originalRoots.isNullOrEmpty() && !commonizedRoots.isNullOrEmpty() && commonRoot != null
                        && (originalRoots + commonRoot).map { it.name }.toSet() == commonizedRoots.map { it.name }.toSet()
            ) {
                "Source module misconfiguration in $testDataDir"
            }

            return Triple(originalRoots, commonizedRoots - commonRoot, commonRoot)
        }

    protected val sourceModuleDescriptors: Pair<Map<InputTarget, ModuleDescriptor>, Map<Target, ModuleDescriptor>>
        get() {
            fun analyzeTarget(targetRoot: File): ModuleDescriptor {
                val environment = createEnvironment(targetRoot.parentFile.parentFile.name)
                val psiFactory = KtPsiFactory(environment.project)

                val psiFiles = targetRoot.walkTopDown()
                    .filter { it.isFile }
                    .map { psiFactory.createFile(it.name, doLoadFile(it)) }
                    .toList()

                return CommonResolverForModuleFactory.analyzeFiles(
                    files = psiFiles,
                    moduleName = environment.moduleName,
                    dependOnBuiltIns = true,
                    languageVersionSettings = environment.configuration.languageVersionSettings,
                    targetPlatform = CommonPlatforms.defaultCommonPlatform
                ) { content ->
                    environment.createPackagePartProvider(content.moduleContentScope)
                }.moduleDescriptor
            }

            val originalModules = sourceModuleRoots.first
                .map { InputTarget(it.name) to analyzeTarget(it) }
                .toMap()

            val commonizedModules = sourceModuleRoots.second
                .map { InputTarget(it.name) to analyzeTarget(it) }
                .toMap()

            val commonModule = OutputTarget(commonizedModules.keys) to analyzeTarget(sourceModuleRoots.third)

            return originalModules to commonizedModules + commonModule
        }

    protected fun doTestSuccessfulCommonization() {
        val (originalModules, commonizedModules) = sourceModuleDescriptors

        val result = runCommonization(originalModules.toCommonizationParameters())
        assertCommonizationPerformed(result)

        val commonTarget = commonizedModules.keys.filterIsInstance<OutputTarget>().single()
        assertEquals(commonTarget, result.commonTarget)

        val commonModuleAsExpected = commonizedModules.getValue(commonTarget)
        val commonModuleByCommonizer = result.modulesByTargets.getValue(commonTarget).single()

        assertValidModule(commonModuleAsExpected)
        assertValidModule(commonModuleByCommonizer)
        assertModulesAreEqual(commonModuleAsExpected, commonModuleByCommonizer, "\"$commonTarget\" target")

        val concreteTargets = commonizedModules.keys - commonTarget
        assertEquals(concreteTargets, result.concreteTargets)

        for (target in concreteTargets) {
            val targetModuleAsExpected = commonizedModules.getValue(target)
            val targetModuleByCommonizer = result.modulesByTargets.getValue(target).single()

            assertValidModule(targetModuleAsExpected)
            assertValidModule(targetModuleByCommonizer)
            assertModulesAreEqual(targetModuleAsExpected, targetModuleByCommonizer, "\"$target\" target")
        }
    }
}
