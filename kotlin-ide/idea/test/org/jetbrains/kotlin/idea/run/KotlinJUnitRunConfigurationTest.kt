/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.ConfigurationFromContext
import com.intellij.execution.junit.TestInClassConfigurationProducer
import com.intellij.openapi.application.PathManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil.updateModel
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import com.intellij.openapi.vfs.JarFileSystem
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.util.PlatformUtils
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.*
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit38ClassRunner::class)
class KotlinJUnitRunConfigurationTest : AbstractRunConfigurationTest() {
    fun testSimple() = withTestFiles {
        if (!PlatformUtils.isIntelliJ()) {
            return
        }

        val projectBaseDir = PlatformTestUtil.getOrCreateProjectBaseDir(project)
        val createResult = configureModule(moduleDirPath("module"), projectBaseDir)
        val testDir = createResult.testDir!!

        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(module, addJdk(testRootDisposable, ::mockJdk))

        try {
            attachJUnitLibrary()

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
        } finally {
            detachJUnitLibrary()
        }
    }

    private fun attachJUnitLibrary() {
        val platformPath = PathManager.getHomePath().replace(File.separatorChar, '/')
        val junitLibraryFile = File("$platformPath/lib/junit-4.12.jar")
        val junitLibraryVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByPath(junitLibraryFile.canonicalPath)!!

        updateModel(module) { model ->
            val editor = NewLibraryEditor()
            editor.name = "JUnit"
            editor.addRoot(JarFileSystem.getInstance().getJarRootForLocalFile(junitLibraryVirtualFile)!!, OrderRootType.CLASSES)
            ConfigLibraryUtil.addLibrary(editor, model)
        }
    }

    private fun detachJUnitLibrary() {
        ConfigLibraryUtil.removeLibrary(module, "JUnit")
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