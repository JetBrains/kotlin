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

import com.intellij.execution.filters.FileHyperlinkInfo
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.fileClasses.NoResolveFileClassesProvider
import org.jetbrains.kotlin.fileClasses.getFileClassFqName
import org.jetbrains.kotlin.idea.core.refactoring.toVirtualFile
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.net.URLClassLoader

public abstract class AbstractKotlinExceptionFilterTest: KotlinCodeInsightTestCase() {
    override fun getTestDataPath() = ""

    protected fun doTest(path: String) {
        val rootDir = File(path)
        val mainFile = File(rootDir, rootDir.name + ".kt")
        rootDir.listFiles().filter { it != mainFile }.forEach { configureByFile(it.canonicalPath) }
        configureByFile(mainFile.canonicalPath)

        val fileText = file.text

        val outDir = project.baseDir.createChildDirectory(this, "out")
        PsiTestUtil.setCompilerOutputPath(module, outDir.url, false)

        MockLibraryUtil.compileKotlin(path, File(outDir.path))

        val stackTraceElement = try {
            val className = NoResolveFileClassesProvider.getFileClassFqName(file as KtFile)
            val clazz = URLClassLoader(arrayOf(URL(outDir.url + "/")), ForTestCompileRuntime.runtimeJarClassLoader()).loadClass(className.asString())
            clazz.getMethod("box")?.invoke(null)
            throw AssertionError("class ${className.asString()} should have box() method and throw exception")
        }
        catch(e: InvocationTargetException) {
            e.targetException.stackTrace[0]
        }

        val filter = KotlinExceptionFilterFactory().create(GlobalSearchScope.allScope(project))
        val prefix = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// PREFIX: ") ?: "at"
        val result = filter.applyFilter("$prefix $stackTraceElement", 0) ?: throw AssertionError("Couldn't apply filter to $stackTraceElement")

        val info = result.firstHyperlinkInfo as FileHyperlinkInfo
        val descriptor = info.descriptor!!

        val expectedFileName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// FILE: ")!!
        val expectedVirtualFile = File(rootDir, expectedFileName).toVirtualFile() ?: throw AssertionError("Couldn't find file: name = $expectedFileName")
        val expectedLineNumber = InTextDirectivesUtils.getPrefixedInt(fileText, "// LINE: ")!!

        // TODO compare virtual files
        assertEquals(expectedFileName, descriptor.file.name)

        val document = FileDocumentManager.getInstance().getDocument(expectedVirtualFile)!!
        val expectedOffset = document.getLineStartOffset(expectedLineNumber - 1)
        assertEquals(expectedOffset, descriptor.offset)
    }
}
