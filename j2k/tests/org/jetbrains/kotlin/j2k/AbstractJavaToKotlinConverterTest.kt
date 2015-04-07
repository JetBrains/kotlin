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

package org.jetbrains.kotlin.j2k

import com.intellij.openapi.util.io.FileUtil
import java.io.File
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.util.Computable
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.kotlin.psi.JetFile
import org.jetbrains.kotlin.idea.caches.resolve.analyzeFullyAndGetResult
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.JetTestUtils

public abstract class AbstractJavaToKotlinConverterTest : LightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        VfsRootAccess.allowRootAccess(JetTestUtils.getHomeDirectory())

        addFile("KotlinApi.kt", "kotlinApi")
        addFile("JavaApi.java", "javaApi")
    }

    override fun tearDown() {
        VfsRootAccess.disallowRootAccess(JetTestUtils.getHomeDirectory())
        super.tearDown()
    }
    
    private fun addFile(fileName: String, dirName: String) {
        addFile(File("j2k/testData/$fileName"), dirName)
    }

    protected fun addFile(file: File, dirName: String): VirtualFile {
        return addFile(FileUtil.loadFile(file, true), file.getName(), dirName)
    }

    protected fun addFile(text: String, fileName: String, dirName: String): VirtualFile {
        return runWriteAction {
            val root = LightPlatformTestCase.getSourceRoot()!!
            val virtualDir = root.findChild(dirName) ?: root.createChildDirectory(null, dirName)
            val virtualFile = virtualDir.createChildData(null, fileName)
            virtualFile.getOutputStream(null)!!.writer().use { it.write(text) }
            virtualFile
        }
    }

    protected fun deleteFile(virtualFile: VirtualFile) {
        runWriteAction { virtualFile.delete(this) }
    }

    protected fun addErrorsDump(jetFile: JetFile): String {
        val diagnostics = jetFile.analyzeFullyAndGetResult().bindingContext.getDiagnostics()
        val errors = diagnostics.filter { it.getSeverity() == Severity.ERROR }
        if (errors.isEmpty()) return jetFile.getText()
        val header = errors.map { "// ERROR: " + DefaultErrorMessages.render(it).replace('\n', ' ') }.joinToString("\n", postfix = "\n")
        return header + jetFile.getText()
    }
}

