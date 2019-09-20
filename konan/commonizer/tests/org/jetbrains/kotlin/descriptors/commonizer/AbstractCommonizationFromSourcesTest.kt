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
            "target_$index" to moduleDescriptor
        }.toMap().toCommonizationParameters()

        fun Map<String, ModuleDescriptor>.toCommonizationParameters() = CommonizationParameters().also {
            forEach { (targetName, moduleDescriptor) ->
                it.addTarget(targetName, listOf(moduleDescriptor))
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
                .resolve("konan/commonizer/testData")
                .resolve(testCaseDir)
                .resolve(testDir)
                .also(::assertIsDirectory)
        }

    protected val sourceModuleRoots: Pair<Set<File>, Set<File>>
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

            check(
                !originalRoots.isNullOrEmpty() && !commonizedRoots.isNullOrEmpty()
                        && (originalRoots.map { it.name } + "common").toSet() == commonizedRoots.map { it.name }.toSet()
            ) {
                "Source module misconfiguration in $testDataDir"
            }

            return originalRoots to commonizedRoots
        }

    protected val sourceModuleDescriptors: Pair<Map<String, ModuleDescriptor>, Map<String, ModuleDescriptor>>
        get() {
            fun analyzeTarget(targetRoot: File): Pair<String, ModuleDescriptor> {
                val environment = createEnvironment(targetRoot.parentFile.parentFile.name)
                val psiFactory = KtPsiFactory(environment.project)

                val psiFiles = targetRoot.walkTopDown()
                    .filter { it.isFile }
                    .map { psiFactory.createFile(it.name, doLoadFile(it)) }
                    .toList()

                val moduleDescriptor = CommonResolverForModuleFactory.analyzeFiles(
                    files = psiFiles,
                    moduleName = environment.moduleName,
                    dependOnBuiltIns = true,
                    languageVersionSettings = environment.configuration.languageVersionSettings
                ) { content ->
                    environment.createPackagePartProvider(content.moduleContentScope)
                }.moduleDescriptor

                return targetRoot.name to moduleDescriptor
            }

            return sourceModuleRoots.first.map(::analyzeTarget).toMap() to sourceModuleRoots.second.map(::analyzeTarget).toMap()
        }

    protected fun doTestSuccessfulCommonization() {
        val (originalModules, commonizedModules) = sourceModuleDescriptors

        val result = runCommonization(originalModules.toCommonizationParameters())
        assertCommonizationPerformed(result)

        val commonModuleAsExpected = commonizedModules.getValue("common")
        val commonModuleByCommonizer = result.commonModules.single()

        assertValidModule(commonModuleAsExpected)
        assertValidModule(commonModuleByCommonizer)
        assertModulesAreEqual(commonModuleAsExpected, commonModuleByCommonizer, "\"common\" target")

        val concreteTargetNames = commonizedModules.keys - "common"
        assertEquals(concreteTargetNames, result.modulesByTargets.keys)

        for (targetName in concreteTargetNames) {
            val targetModuleAsExpected = commonizedModules.getValue(targetName)
            val targetModuleByCommonizer = result.modulesByTargets.getValue(targetName).single()

            assertValidModule(targetModuleAsExpected)
            assertValidModule(targetModuleByCommonizer)
            assertModulesAreEqual(targetModuleAsExpected, targetModuleByCommonizer, "\"$targetName\" target")
        }
    }
}
