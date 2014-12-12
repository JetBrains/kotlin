/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.j2k.test

import com.intellij.openapi.util.io.FileUtil
import java.io.File
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.fixtures.LightCodeInsightFixtureTestCase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.util.Computable
import junit.framework.TestCase
import org.jetbrains.jet.j2k.translateToKotlin
import com.intellij.openapi.util.Disposer
import org.jetbrains.jet.j2k.JavaToKotlinTranslator
import org.jetbrains.jet.lang.diagnostics.Severity
import org.jetbrains.jet.lang.diagnostics.rendering.DefaultErrorMessages
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.plugin.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.jet.JetTestCaseBuilder
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess

public abstract class AbstractJavaToKotlinConverterTest : LightCodeInsightFixtureTestCase() {
    override fun setUp() {
        super.setUp()

        VfsRootAccess.allowRootAccess(JetTestCaseBuilder.getHomeDirectory())

        addFile("KotlinApi.kt", "kotlinApi")
        addFile("JavaApi.java", "javaApi")
    }

    override fun tearDown() {
        VfsRootAccess.disallowRootAccess(JetTestCaseBuilder.getHomeDirectory())
        super.tearDown()
    }
    
    private fun addFile(fileName: String, packageName: String) {
        addFile(File("j2k/tests/testData/$fileName"), packageName)
    }

    protected fun addFile(file: File, dirName: String): VirtualFile {
        return addFile(FileUtil.loadFile(file, true), file.getName(), dirName)
    }

    protected fun addFile(text: String, fileName: String, dirName: String): VirtualFile {
        return ApplicationManager.getApplication()!!.runWriteAction(object: Computable<VirtualFile> {
            override fun compute(): VirtualFile? {
                val root = LightPlatformTestCase.getSourceRoot()!!
                val virtualDir = root.findChild(dirName) ?: root.createChildDirectory(null, dirName)
                val virtualFile = virtualDir.createChildData(null, fileName)!!
                virtualFile.getOutputStream(null)!!.writer().use { it.write(text) }
                return virtualFile
            }
        })
    }

    protected fun deleteFile(virtualFile: VirtualFile) {
        ApplicationManager.getApplication()!!.runWriteAction { virtualFile.delete(this) }
    }

    protected fun addErrorsDump(jetFile: JetFile): String {
        val diagnostics = jetFile.analyzeFullyAndGetResult().bindingContext.getDiagnostics()
        val errors = diagnostics.filter { it.getSeverity() == Severity.ERROR }
        if (errors.isEmpty()) return jetFile.getText()
        val header = errors.map { "// ERROR: " + DefaultErrorMessages.render(it).replace('\n', ' ') }.joinToString("\n", postfix = "\n")
        return header + jetFile.getText()
    }
}

public abstract class AbstractJavaToKotlinConverterForWebDemoTest() : TestCase() {
    public fun doTest(javaPath: String) {
        try {
            val fileContents = FileUtil.loadFile(File(javaPath), true)
            translateToKotlin(fileContents)
        }
        finally {
            Disposer.dispose(JavaToKotlinTranslator.DISPOSABLE)
        }
    }
}
