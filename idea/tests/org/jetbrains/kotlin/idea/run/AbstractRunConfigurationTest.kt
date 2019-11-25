/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.*
import java.io.File

abstract class AbstractRunConfigurationTest : @Suppress("DEPRECATION") KotlinCodeInsightTestCase() {
    fun getTestProject() = myProject!!
    override fun getModule() = myModule!!

    protected fun configureModule(moduleDir: String, outputParentDir: VirtualFile, configModule: Module = module): CreateModuleResult {
        val srcPath = "$moduleDir/src"
        val srcDir: VirtualFile?

        if (File(srcPath).exists()) {
            srcDir = PsiTestUtil.createTestProjectStructure(project, configModule, srcPath, myFilesToDelete, false)
            PsiTestUtil.addSourceRoot(module, srcDir, false)
        } else {
            srcDir = null
        }

        val testPath = "$moduleDir/test"
        val testDir: VirtualFile?

        if (File(testPath).exists()) {
            testDir = if (srcDir == null) {
                PsiTestUtil.createTestProjectStructure(project, configModule, testPath, myFilesToDelete, false)
            } else {
                val canonicalPath = File(testPath).canonicalPath.replace(File.separatorChar, '/')
                LocalFileSystem.getInstance().refreshAndFindFileByPath(canonicalPath)
            }

            if (testDir != null) {
                PsiTestUtil.addSourceRoot(module, testDir, true)
            }
        } else {
            testDir = null
        }

        val (srcOutDir, testOutDir) = runWriteAction {
            val outDir = outputParentDir.createChildDirectory(this, "out")
            val srcOutDir = outDir.createChildDirectory(this, "production")
            val testOutDir = outDir.createChildDirectory(this, "test")

            PsiTestUtil.setCompilerOutputPath(configModule, srcOutDir.url, false)
            PsiTestUtil.setCompilerOutputPath(configModule, testOutDir.url, true)

            Pair(srcOutDir, testOutDir)
        }

        PsiDocumentManager.getInstance(getTestProject()).commitAllDocuments()

        return CreateModuleResult(configModule, srcDir, testDir, srcOutDir, testOutDir)
    }

    protected class CreateModuleResult(
        val module: Module,
        val srcDir: VirtualFile?,
        val testDir: VirtualFile?,
        val srcOutputDir: VirtualFile,
        val testOutputDir: VirtualFile
    )

    protected fun moduleDirPath(moduleName: String) = "${testDataPath}${getTestName(false)}/$moduleName"
    override fun getTestProjectJdk() = mockJdk()
}