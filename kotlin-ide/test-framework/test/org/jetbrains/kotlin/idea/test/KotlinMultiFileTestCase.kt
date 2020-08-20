/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

import com.intellij.ide.highlighter.ModuleFileType
import com.intellij.openapi.Disposable
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Ref
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.psi.PsiDocumentManager
import com.intellij.refactoring.MultiFileTestCase
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.PsiTestUtil
import com.intellij.util.ThrowableRunnable
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.KotlinTestUtils.*
import java.io.File

abstract class KotlinMultiFileTestCase : MultiFileTestCase() {
    protected var isMultiModule = false
    private var vfsDisposable: Ref<Disposable>? = null

    override fun setUp() {
        super.setUp()
        vfsDisposable = allowProjectRootAccess(this)

        runWriteAction {
            val mockJdk16 = IdeaTestUtil.getMockJdk16()
            PluginTestCaseBase.addJdk(testRootDisposable) { mockJdk16 }
            ProjectRootManager.getInstance(project).projectSdk = mockJdk16
        }
    }

    final override fun getTestDataPath(): String {
        return toSlashEndingDirPath(getTestDataDirectory().absolutePath)
    }

    protected open fun getTestDataDirectory(): File {
        return File(super.getTestDataPath())
    }

    protected fun getTestDirName(lowercaseFirstLetter: Boolean): String {
        val testName = getTestName(lowercaseFirstLetter)
        val endIndex = testName.lastIndexOf('_')
        if (endIndex < 0) return testName
        return testName.substring(0, endIndex).replace('_', '/')
    }

    protected fun doTestCommittingDocuments(action: (VirtualFile, VirtualFile?) -> Unit) {
        super.doTest(
            { rootDir, rootAfter ->
                action(rootDir, rootAfter)

                PsiDocumentManager.getInstance(project!!).commitAllDocuments()
                FileDocumentManager.getInstance().saveAllDocuments()
            }, getTestDirName(true)
        )
    }

    override fun prepareProject(rootDir: VirtualFile) {
        if (isMultiModule) {
            val model = ModuleManager.getInstance(project).modifiableModel

            VfsUtilCore.visitChildrenRecursively(
                rootDir,
                object : VirtualFileVisitor<Any>() {
                    override fun visitFile(file: VirtualFile): Boolean {
                        if (!file.isDirectory && file.name.endsWith(ModuleFileType.DOT_DEFAULT_EXTENSION)) {
                            model.loadModule(file.path)
                            return false
                        }

                        return true
                    }
                }
            )

            runWriteAction { model.commit() }
        } else {
            PsiTestUtil.addSourceContentToRoots(myModule, rootDir)
        }
    }

    override fun tearDown() {
        runAll(
            ThrowableRunnable { disposeVfsRootAccess(vfsDisposable) },
            ThrowableRunnable { super.tearDown() },
        )
    }
}