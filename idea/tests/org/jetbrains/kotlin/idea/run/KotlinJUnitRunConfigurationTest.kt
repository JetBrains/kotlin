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
import com.intellij.util.PlatformUtils
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.*
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith
import java.io.File

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class KotlinJUnitRunConfigurationTest : AbstractRunConfigurationTest() {
    fun testSimple() {
        if (!PlatformUtils.isIntelliJ()) {
            return
        }

        val createResult = configureModule(moduleDirPath("module"), getTestProject().baseDir!!)
        val testDir = createResult.testDir!!

        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(module, addJdk(testRootDisposable, ::mockJdk))

        try {
            attachJUnitLibrary()

            val javaFile = testDir.findChild("MyJavaTest.java")!!
            val kotlinFile = testDir.findChild("MyKotlinTest.kt")!!

            val javaClassConfiguration = getConfiguration(javaFile, project, "MyTest")
            javaClassConfiguration.isProducedBy(TestInClassConfigurationProducer::class.java)
            assert(javaClassConfiguration.configuration !is KotlinJUnitConfiguration)

            val javaMethodConfiguration = getConfiguration(javaFile, project, "testA")
            javaMethodConfiguration.isProducedBy(TestInClassConfigurationProducer::class.java)
            assert(javaMethodConfiguration.configuration !is KotlinJUnitConfiguration)

            val kotlinClassConfiguration = getConfiguration(kotlinFile, project, "MyKotlinTest")
            kotlinClassConfiguration.isProducedBy(KotlinJUnitRunConfigurationProducer::class.java)
            assert(kotlinClassConfiguration.configuration is KotlinJUnitConfiguration)

            val kotlinFunctionConfiguration = getConfiguration(kotlinFile, project, "testA")
            kotlinFunctionConfiguration.isProducedBy(KotlinJUnitRunConfigurationProducer::class.java)
            assert(kotlinFunctionConfiguration.configuration is KotlinJUnitConfiguration)
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

    override fun getTestDataPath() = getTestDataPathBase() + "/runConfigurations/junit/"
}

fun getConfiguration(file: VirtualFile, project: Project, pattern: String): ConfigurationFromContext {
    val psiFile = PsiManager.getInstance(project).findFile(file) ?: error("PsiFile not found for $file")
    val offset = psiFile.text.indexOf(pattern)
    val psiElement = psiFile.findElementAt(offset)
    val location = PsiLocation(psiElement)
    val context = ConfigurationContext.createEmptyContextForLocation(location)
    return context.configurationsFromContext.orEmpty().singleOrNull() ?: error("Configuration not found for pattern $pattern")
}