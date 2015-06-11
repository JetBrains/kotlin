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

package org.jetbrains.kotlin.idea.filters

import com.intellij.execution.filters.OpenFileHyperlinkInfo
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.refactoring.MultiFileTestCase
import com.intellij.testFramework.PlatformTestCase
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.UsefulTestCase
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils.getPackageClassFqName
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils.getPackageClassName
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils.getPackagePartFqName
import org.jetbrains.kotlin.name.FqName
import java.io.File

public class JetExceptionFilterTest : MultiFileTestCase() {
    private var rootDir: VirtualFile? = null

    override fun tearDown() {
        rootDir = null
        super.tearDown()
    }

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase()
    override fun getTestRoot() = "/filters/exceptionFilter/"

    private fun configure() {
        val path = getTestDataPath() + getTestRoot() + getTestName(true)

        rootDir = PsiTestUtil.createTestProjectStructure(myProject, myModule, path, PlatformTestCase.myFilesToDelete, false)
        prepareProject(rootDir)
        PsiDocumentManager.getInstance(myProject).commitAllDocuments()
    }

    private fun createStackTraceElementLine(prefix: String, fileName: String, className: String, lineNumber: Int): String {
        // Method name doesn't matter
        val methodName = "foo"

        // File's last name appears in stack trace
        val fileLastName = File(fileName).getName()

        val element = StackTraceElement(className, methodName, fileLastName, lineNumber)
        return prefix + element + "\n"
    }

    private fun doTest(fileName: String, lineNumber: Int, className: (VirtualFile) -> String, linePrefix: String = "\tat ") {
        if (rootDir == null) {
            configure()
        }
        assert(rootDir != null)

        val filter = JetExceptionFilterFactory().create(GlobalSearchScope.allScope(myProject))

        val expectedFile = VfsUtilCore.findRelativeFile(fileName, rootDir)
        TestCase.assertNotNull(expectedFile)

        val line = createStackTraceElementLine(linePrefix, fileName, className.invoke(expectedFile), lineNumber)
        val result = filter.applyFilter(line, 0)

        TestCase.assertNotNull(result)
        val info = result.getFirstHyperlinkInfo()
        TestCase.assertNotNull(info)
        UsefulTestCase.assertInstanceOf(info, javaClass<OpenFileHyperlinkInfo>())
        val descriptor = (info as OpenFileHyperlinkInfo).getDescriptor()
        TestCase.assertNotNull(descriptor)

        TestCase.assertEquals(expectedFile, descriptor.getFile())

        val document = FileDocumentManager.getInstance().getDocument(expectedFile)
        TestCase.assertNotNull(document)
        val expectedOffset = document.getLineStartOffset(lineNumber - 1)
        TestCase.assertEquals(expectedOffset, descriptor.getOffset())
    }

    public fun testBreakpointReachedAt() {
        doTest("breakpointReachedAt.kt", 2, { getPackageClassName(FqName.ROOT) }, linePrefix = "Breakpoint reached at ")
    }

    public fun testSimple() {
        doTest("simple.kt", 2, { getPackageClassName(FqName.ROOT) })
    }

    public fun testKt2489() {
        val packageClassFqName = getPackageClassFqName(FqName.ROOT)
        doTest("a.kt", 3, { file -> "" + getPackagePartFqName(packageClassFqName, file) + "\$a\$f\$1" })
        doTest("main.kt", 3, { file -> "" + getPackagePartFqName(packageClassFqName, file) + "\$main\$f\$1" })
    }

    public fun testMultiSameName() {
        val packageClassFqName = getPackageClassFqName(FqName("multiSameName"))
        // The order and the exact names do matter here
        doTest("1/foo.kt", 4, { file -> "" + getPackagePartFqName(packageClassFqName, file) + "\$foo\$f\$1" })
        doTest("2/foo.kt", 4, { file -> "" + getPackagePartFqName(packageClassFqName, file) + "\$foo\$f\$1" })
    }
}
