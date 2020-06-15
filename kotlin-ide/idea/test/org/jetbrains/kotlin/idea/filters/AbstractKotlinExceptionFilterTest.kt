/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinCompilerStandalone
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

        val extraOptions = InTextDirectivesUtils.findListWithPrefixes(fileText, "// !LANGUAGE: ").map { "-XXLanguage:$it" }
        val classLoader: URLClassLoader
        if (InTextDirectivesUtils.getPrefixedBoolean(fileText, "// WITH_MOCK_LIBRARY: ") ?: false) {
            if (MOCK_LIBRARY_JAR == null) {
                val sources = listOf(File(MOCK_LIBRARY_SOURCES))
                MOCK_LIBRARY_JAR = KotlinCompilerStandalone(sources).compile()
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

            val sources = listOf(File(path))
            val classpath = listOf(File(mockLibraryPath))
            KotlinCompilerStandalone(sources, target = File(outDir.path), options = extraOptions, classpath = classpath).compile()

            classLoader = URLClassLoader(
                arrayOf(URL(outDir.url + "/"), mockLibraryJar.toURI().toURL()),
                ForTestCompileRuntime.runtimeJarClassLoader()
            )
        } else {
            val sources = listOf(File(path))
            KotlinCompilerStandalone(sources, target = File(outDir.path), options = extraOptions)

            classLoader = URLClassLoader(
                arrayOf(URL(outDir.url + "/")),
                ForTestCompileRuntime.runtimeJarClassLoader()
            )
        }

        val stackTraceElement = try {
            val className = JvmFileClassUtil.getFileClassInfoNoResolve(file as KtFile).fileClassFqName
            val clazz = classLoader.loadClass(className.asString())
            clazz.getMethod("box")?.invoke(null)
            throw AssertionError("class ${className.asString()} should have box() method and throw exception")
        } catch (e: InvocationTargetException) {
            e.targetException.stackTrace[0]
        }

        val filter = KotlinExceptionFilterFactory().create(GlobalSearchScope.allScope(project))
        val prefix = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// PREFIX: ") ?: "at"
        val stackTraceString = stackTraceElement.toString()
        val text = "$prefix $stackTraceString"
        var result = filter.applyFilter(text, text.length)
            ?: throw AssertionError("Couldn't apply filter to $stackTraceElement")

        if (InTextDirectivesUtils.isDirectiveDefined(fileText, "SMAP_APPLIED")) {
            val fileHyperlinkInfo = result.firstHyperlinkInfo as FileHyperlinkInfo
            val descriptor = fileHyperlinkInfo.descriptor!!

            val file = descriptor.file
            val line = descriptor.line + 1

            val newStackString = stackTraceString
                .replace(mainFile.name, file.name)
                .replace(Regex(":\\d+\\)"), ":$line)")

            val newLine = "$prefix $newStackString"
            result = filter.applyFilter(newLine, newLine.length) ?: throw AssertionError("Couldn't apply filter to $stackTraceElement")
        }

        val info = result.firstHyperlinkInfo as FileHyperlinkInfo
        val descriptor = if (InTextDirectivesUtils.isDirectiveDefined(fileText, "NAVIGATE_TO_CALL_SITE"))
            (info as? InlineFunctionHyperLinkInfo)?.callSiteDescriptor
                ?: throw AssertionError("`$stackTraceString` did not resolve to an inline function call")
        else
            info.descriptor!!

        val expectedFileName = InTextDirectivesUtils.findStringWithPrefixes(fileText, "// FILE: ")!!
        val expectedVirtualFile = File(rootDir, expectedFileName).toVirtualFile()
            ?: File(MOCK_LIBRARY_SOURCES, expectedFileName).toVirtualFile()
            ?: throw AssertionError("Couldn't find file: name = $expectedFileName")
        val expectedLineNumber = InTextDirectivesUtils.getPrefixedInt(fileText, "// LINE: ")!!


        val document = FileDocumentManager.getInstance().getDocument(expectedVirtualFile)!!
        val expectedOffset = document.getLineStartOffset(expectedLineNumber - 1)

        // TODO compare virtual files
        assertEquals(
            "Wrong result for line $stackTraceElement",
            "$expectedFileName:$expectedOffset",
            descriptor.file.name + ":" + descriptor.offset
        )
    }
}
