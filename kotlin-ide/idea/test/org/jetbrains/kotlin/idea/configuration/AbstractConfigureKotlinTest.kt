/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.configuration

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.testFramework.IdeaTestUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts
import org.jetbrains.kotlin.idea.configuration.KotlinWithLibraryConfigurator.FileState
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

abstract class AbstractConfigureKotlinTest : AbstractConfigureKotlinTestBase() {
    protected fun doTestConfigureModulesWithNonDefaultSetup(configurator: KotlinWithLibraryConfigurator) {
        assertNoFilesInDefaultPaths()

        modules.forEach { assertNotConfigured(it, configurator) }
        configurator.configure(myProject, emptyList())
        modules.forEach { assertProperlyConfigured(it, configurator) }

        assertNoFilesInDefaultPaths()
    }

    protected fun doTestSingleJvmModule(jarState: FileState) {
        doTestSingleModule(jarState, jvmConfigurator)
    }

    protected fun doTestSingleJsModule(jarState: FileState) {
        doTestSingleModule(jarState, jsConfigurator)
    }

    private fun doTestSingleModule(jarState: FileState, configurator: KotlinWithLibraryConfigurator) {
        assertNotConfigured(module, configurator)
        configure(module, jarState, configurator)
        assertProperlyConfigured(module, configurator)
    }

    protected fun assertProperlyConfigured(module: Module, configurator: KotlinWithLibraryConfigurator) {
        assertConfigured(module, configurator)
        assertNotConfigured(module, getOppositeConfigurator(configurator))
    }

    protected fun assertNotConfigured(module: Module, configurator: KotlinWithLibraryConfigurator) {
        TestCase.assertFalse(
            String.format("Module %s should not be configured as %s Module", module.name, configurator.presentableText),
            configurator.isConfigured(module)
        )
    }

    protected fun assertConfigured(module: Module, configurator: KotlinWithLibraryConfigurator) {
        TestCase.assertTrue(
            String.format("Module %s should be configured with configurator '%s'", module.name, configurator.presentableText),
            configurator.isConfigured(module)
        )
    }

    private fun assertNoFilesInDefaultPaths() {
        assertDoesntExist(File(jvmConfigurator.getDefaultPathToJarFile(project)))
        assertDoesntExist(File(jsConfigurator.getDefaultPathToJarFile(project)))
    }

    private fun configure(
        modules: List<Module>,
        runtimeState: FileState,
        configurator: KotlinWithLibraryConfigurator,
        jarFromDist: String,
        jarFromTemp: String
    ) {
        val project = modules.first().project
        val collector = createConfigureKotlinNotificationCollector(project)

        val pathToJar = getPathToJar(runtimeState, jarFromDist, jarFromTemp)
        for (module in modules) {
            configurator.configureModule(module, pathToJar, pathToJar, collector, runtimeState)
        }
        collector.showNotification()
    }

    private fun getPathToJar(runtimeState: FileState, jarFromDist: String, jarFromTemp: String) = when (runtimeState) {
        FileState.EXISTS -> jarFromDist
        FileState.COPY -> jarFromTemp
        FileState.DO_NOT_COPY -> jarFromDist
    }

    companion object {
        private val pathToExistentRuntimeJar: String
            get() = KotlinArtifacts.instance.kotlinStdlib.parent

        private val pathToExistentJsJar: String
            get() = KotlinArtifacts.instance.kotlinStdlibJs.parent
    }

    protected fun configure(module: Module, jarState: FileState, configurator: KotlinProjectConfigurator) {
        if (configurator is KotlinJavaModuleConfigurator) {
            configure(
                listOf(module), jarState,
                configurator as KotlinWithLibraryConfigurator,
                pathToExistentRuntimeJar, pathToNonexistentRuntimeJar
            )
        }

        if (configurator is KotlinJsModuleConfigurator) {
            configure(
                listOf(module), jarState,
                configurator as KotlinWithLibraryConfigurator,
                pathToExistentJsJar, pathToNonexistentJsJar
            )
        }
    }

    private val pathToNonexistentRuntimeJar: String
        get() = KotlinTestUtils.tmpDirForReusableFolder("stdlib").resolve(PathUtil.KOTLIN_JAVA_STDLIB_JAR).path

    private val pathToNonexistentJsJar: String
        get() = KotlinTestUtils.tmpDirForReusableFolder("stdlib").resolve(PathUtil.KOTLIN_JAVA_STDLIB_JAR).path

    override fun getTestProjectJdk(): Sdk = IdeaTestUtil.createMockJdk("1.8", IdeaTestUtil.getMockJdk18Path().path)
}
