/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.stubs

import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.PsiManager
import com.intellij.openapi.vfs.VirtualFileFilter
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.JetFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import com.intellij.openapi.vfs.VfsUtilCore
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import kotlin.test.fail

public object AstAccessControl {
    public val ALLOW_AST_ACCESS_DIRECTIVE: String = "ALLOW_AST_ACCESS"

    // Please provide at least one test that fails ast switch check (shouldFail should be true for at least one test)
    // This kind of inconvenience is justified by the fact that the check can be invalidated by slight misconfiguration of the test
    // leading to all tests passing
    fun testWithControlledAccessToAst(shouldFail: Boolean, project: Project, disposable: Disposable, testBody: () -> Unit) {
        testWithControlledAccessToAst(shouldFail, listOf(), project, disposable, testBody)
    }

    fun testWithControlledAccessToAst(
            shouldFail: Boolean, allowedFile: VirtualFile,
            project: Project, disposable: Disposable, testBody: () -> Unit
    ) {
        testWithControlledAccessToAst(shouldFail, listOf(allowedFile), project, disposable, testBody)
    }

    fun testWithControlledAccessToAst(
            shouldFail: Boolean, allowedFiles: List<VirtualFile>,
            project: Project, disposable: Disposable, testBody: () -> Unit
    ) {
        setFilter(allowedFiles, disposable, project)
        performTest(shouldFail, testBody)
    }

    private fun setFilter(allowedFiles: List<VirtualFile>, disposable: Disposable, project: Project) {
        val manager = (PsiManager.getInstance(project) as PsiManagerImpl)
        val filter = VirtualFileFilter {
            file ->
            if (file!!.getFileType() != JetFileType.INSTANCE || file in allowedFiles) {
                false
            }
            else {
                val text = VfsUtilCore.loadText(file)
                !InTextDirectivesUtils.isDirectiveDefined(text, ALLOW_AST_ACCESS_DIRECTIVE)
            }
        }
        manager.setAssertOnFileLoadingFilter(filter, disposable)
    }

    private fun performTest(shouldFail: Boolean, testBody: () -> Unit) {
        try {
            testBody()
            if (shouldFail) {
                fail("This failure means that that a test that should fail (by triggering ast switch) in fact did not.\n" +
                     "This could happen for the following reasons:\n" +
                     "1. This kind of operation no longer trigger ast switch, choose better indicator test case." +
                     "2. Test is now misconfigured and no longer checks for ast switch, reconfigure the test.")
            }
        }
        catch (e: Throwable) {
            if (!shouldFail) {
                throw e
            }
        }
    }
}
