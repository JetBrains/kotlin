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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.refactoring.toVirtualFile
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.net.URLClassLoader

private var MOCK_LIBRARY_JAR: File? = null
private val MOCK_LIBRARY_SOURCES = PluginTestCaseBase.getTestDataPathBase() + "/debugger/mockLibraryForExceptionFilter"

abstract class AbstractKotlinExceptionFilterTest : KotlinCodeInsightTestCase() {
    override fun getTestDataPath() = ""

    protected fun doTest(path: String) {
        val rootDir = File(path)
        val mainFile = File(rootDir, rootDir.name + ".kt")
        rootDir.listFiles().filter { it != mainFile }.forEach { configureByFile(it.canonicalPath) }
        configureByFile(mainFile.canonicalPath)

        val fileText = file.text

        val outDir = runWriteAction {
            project.baseDir.findChild("out") ?: project.baseDir.createChildDirectory(this, "out")
        }
        PsiTestUtil.setCompilerOutputPath(module, outDir.url, false)

        val classLoader: URLClassLoader
        if (InTextDirectivesUtils.getPrefixedBoolean(fileText, "// WITH_MOCK_LIBRARY: ") ?: false) {
            if (MOCK_LIBRARY_JAR == null) {
                MOCK_LIBRARY_JAR = MockLibraryUtil.compileJvmLibraryToJar(MOCK_LIBRARY_SOURCES, "mockLibrary", addSources = true)
            }

            val mockLibraryJar = MOCK_LIBRARY_JAR ?: throw AssertionError("Mock library JAR is null")
            val mockLibraryPath = FileUtilRt.toSystemIndependentName(mockLibraryJar.canonicalPath)
            val libRootUrl = "jar://$mockLibraryPath!/"

            ApplicationManager.getApplication().runWriteAction {
                val moduleModel = ModuleRootManager.getInstance(myModule).modifiableModel
                with(moduleModel.moduleLibraryTable.modifiableModel.createLibrary("mockLibrary").modifiableModel) {
                    addRoot(libRootUrl, OrderRootType.CLASSES)
                    addRoot(libRootUrl + "src/", OrderRootType.SOURCES)
                    commit()
                }
                moduleModel.commit()
            }
            MockLibraryUtil.compileKotlin(path, File(outDir.path), extraClasspath = mockLibraryPath)
            classLoader = URLClassLoader(
                    arrayOf(URL(outDir.url + "/"), mockLibraryJar.toURI().toURL()),
                    ForTestCompileRuntime.runtimeJarClassLoader())
        }
        else {
            MockLibraryUtil.compileKotlin(path, File(outDir.path))
            classLoader = URLClassLoader(
                    arrayOf(URL(outDir.url + "/")),
                    ForTestCompileRuntime.runtimeJarClassLoader())
        }

        val stackTraceElement = try {
            val className = JvmFileClassUtil.getFileClassInfoNoResolve(file as KtFile).fileClassFqName
            val clazz = classLoader.loadClass(className.asString())
            clazz.getMethod("box")?.invoke(null)
            throw AssertionError("class ${className.asString()} should have box() method and throw exception")
        }
        catch(e: InvocationTargetException) {
            e.targetException.stackTrace[0]
        }

        val filter = KotlinExceptionFilterFactory().create(GlobalSearchScope.allScope(project))
        val prefix = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// PREFIX: ") ?: "at"
        val stackTraceString = stackTraceElement.toString()
        var result = filter.applyFilter("$prefix $stackTraceString", 0) ?: throw AssertionError("Couldn't apply filter to $stackTraceElement")

        if (InTextDirectivesUtils.isDirectiveDefined(fileText, "SMAP_APPLIED")) {
            val fileHyperlinkInfo = result.firstHyperlinkInfo as FileHyperlinkInfo
            val descriptor = fileHyperlinkInfo.descriptor!!

            val file = descriptor.file
            val line = descriptor.line + 1

            val newStackString = stackTraceString
                    .replace(mainFile.name, file.name)
                    .replace(Regex("\\:\\d+\\)"), ":$line)")

            result = filter.applyFilter("$prefix $newStackString", 0) ?: throw AssertionError("Couldn't apply filter to $stackTraceElement")
        }

        val info = result.firstHyperlinkInfo as FileHyperlinkInfo
        val descriptor = info.descriptor!!

        val expectedFileName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// FILE: ")!!
        val expectedVirtualFile = File(rootDir, expectedFileName).toVirtualFile()
                                        ?: File(MOCK_LIBRARY_SOURCES, expectedFileName).toVirtualFile()
                                        ?: throw AssertionError("Couldn't find file: name = $expectedFileName")
        val expectedLineNumber = InTextDirectivesUtils.getPrefixedInt(fileText, "// LINE: ")!!


        val document = FileDocumentManager.getInstance().getDocument(expectedVirtualFile)!!
        val expectedOffset = document.getLineStartOffset(expectedLineNumber - 1)

        // TODO compare virtual files
        assertEquals("Wrong result for line $stackTraceElement", expectedFileName + ":" + expectedOffset, descriptor.file.name + ":" + descriptor.offset)
    }
}
