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
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.test.KotlinTestUtils.*
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import java.io.File

@Suppress("MemberVisibilityCanBePrivate")
abstract class AbstractCommonizationFromSourcesTest : KtUsefulTestCase() {
    companion object {
        init {
            System.setProperty("java.awt.headless", "true")
        }

        fun List<ModuleDescriptor>.eachModuleAsTarget() = CommonizationParameters().also {
            forEachIndexed { index, module ->
                it.addTarget("target_$index", listOf(module))
            }
        }
    }

    fun assertIsDirectory(file: File) {
        assertTrue("Not a directory: $file", file.isDirectory)
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

    protected val sourceModuleRoots: List<File>
        get() {
            val testDataDir = testDataDir
            val roots = testDataDir.listFiles()?.toList()

            if (roots.isNullOrEmpty())
                error("No source module roots found in $testDataDir")

            roots.forEach(::assertIsDirectory)

            return roots
        }

    protected val sourceModuleDescriptors: List<ModuleDescriptor>
        get() {
            return sourceModuleRoots.map { root ->
                val environment = createEnvironment(root.parentFile.name)
                val psiFactory = KtPsiFactory(environment.project)

                val psiFiles = root.walkTopDown()
                    .filter { it.isFile }
                    .map { psiFactory.createFile(it.name, doLoadFile(it)) }
                    .toList()

                CommonResolverForModuleFactory.analyzeFiles(
                    files = psiFiles,
                    moduleName = environment.moduleName,
                    dependOnBuiltIns = true,
                    languageVersionSettings = environment.configuration.languageVersionSettings
                ) { content ->
                    environment.createPackagePartProvider(content.moduleContentScope)
                }.moduleDescriptor
            }
        }
}
