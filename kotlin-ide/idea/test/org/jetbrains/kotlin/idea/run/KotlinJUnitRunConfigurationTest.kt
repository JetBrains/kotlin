/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.junit.TestInClassConfigurationProducer
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.idea.test.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.idea.test.MockLibraryFacility
import org.jetbrains.kotlin.idea.test.runAll
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith

@RunWith(JUnit38ClassRunner::class)
class KotlinJUnitRunConfigurationTest : AbstractRunConfigurationTest() {
    private lateinit var mockLibraryFacility: MockLibraryFacility

    override fun setUp() {
        super.setUp()
        mockLibraryFacility = MockLibraryFacility(testDataDirectory.resolve("mock"))
        mockLibraryFacility.setUp(module)
    }

    override fun tearDown() {
        runAll(
            ThrowableRunnable { mockLibraryFacility.tearDown(module) },
            ThrowableRunnable { super.tearDown() }
        )
    }

    fun testSimple() {
        configureProject()
        val configuredModule = configuredModules.single()

        val testDir = configuredModule.testDir!!

        val javaFile = testDir.findChild("MyJavaTest.java")!!
        val kotlinFile = testDir.findChild("MyKotlinTest.kt")!!

        val javaClassConfiguration = getConfiguration(javaFile, project, "MyTest")
        assert(javaClassConfiguration.isProducedBy(TestInClassConfigurationProducer::class.java))
        assert(javaClassConfiguration.configuration.name == "MyJavaTest")

        val javaMethodConfiguration = getConfiguration(javaFile, project, "testA")
        assert(javaMethodConfiguration.isProducedBy(TestInClassConfigurationProducer::class.java))
        assert(javaMethodConfiguration.configuration.name == "MyJavaTest.testA")

        val kotlinClassConfiguration = getConfiguration(kotlinFile, project, "MyKotlinTest")
        assert(kotlinClassConfiguration.isProducedBy(KotlinJUnitRunConfigurationProducer::class.java))
        assert(kotlinClassConfiguration.configuration.name == "MyKotlinTest")

        val kotlinFunctionConfiguration = getConfiguration(kotlinFile, project, "testA")
        assert(kotlinFunctionConfiguration.isProducedBy(KotlinJUnitRunConfigurationProducer::class.java))
        assert(kotlinFunctionConfiguration.configuration.name == "MyKotlinTest.testA")
    }

    override fun getTestDataDirectory() = IDEA_TEST_DATA_DIR.resolve("runConfigurations/junit")
}

fun getConfiguration(file: VirtualFile, project: Project, pattern: String): ConfigurationFromContext {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: error("PsiFile not found for $file")
    val offset = psiFile.text.indexOf(pattern)
    val psiElement = psiFile.findElementAt(offset)
    val location = PsiLocation(psiElement)
    val context = ConfigurationContext.createEmptyContextForLocation(location)
    return context.configurationsFromContext.orEmpty().singleOrNull() ?: error("Configuration not found for pattern $pattern")
}