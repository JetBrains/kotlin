/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.filters

import com.intellij.execution.filters.FileHyperlinkInfo
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.idea.core.util.toVirtualFile
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinCompilerStandalone
import java.io.File
import java.lang.reflect.InvocationTargetException
import java.net.URL
import java.net.URLClassLoader

abstract class AbstractKotlinExceptionFilterTest : KotlinCodeInsightTestCase() {
    private val MOCK_LIBRARY_NAME = "mockLibrary"
    private val MOCK_LIBRARY_SOURCES = IDEA_TEST_DATA_DIR.resolve("debugger/mockLibraryForExceptionFilter")

    protected fun doTest(path: String) {
        val rootDir = File(path)
        val mainFile = File(rootDir, rootDir.name + ".kt")
        rootDir.listFiles().orEmpty().filter { it != mainFile }.forEach { configureByFile(it.canonicalPath) }
        configureByFile(mainFile.canonicalPath)

        val fileText = file.text

        val outDir = runWriteAction {
            project.baseDir.findChild("out") ?: project.baseDir.createChildDirectory(this, "out")
        }

        // Clean the output directory as it shared between tests
        File(outDir.path).listFiles()?.forEach { it.deleteRecursively() }

        PsiTestUtil.setCompilerOutputPath(module, outDir.url, false)

        val extraOptions = InTextDirectivesUtils.findListWithPrefixes(fileText, "// !LANGUAGE: ").map { "-XXLanguage:$it" }
        val classLoader: URLClassLoader
        if (InTextDirectivesUtils.getPrefixedBoolean(fileText, "// WITH_MOCK_LIBRARY: ") == true) {
            val mockLibraryJar = compileMockLibrary()
            val mockLibraryPath = FileUtilRt.toSystemIndependentName(mockLibraryJar.canonicalPath)
            val libRootUrl = "jar://$mockLibraryPath!/"

            val editor = NewLibraryEditor().apply {
                name = MOCK_LIBRARY_NAME
                addRoot(libRootUrl, OrderRootType.CLASSES)
                addRoot("file://" + MOCK_LIBRARY_SOURCES.path, OrderRootType.SOURCES)
            }

            ConfigLibraryUtil.addLibrary(editor, module)

            val sources = listOf(File(path))
            val classpath = listOf(File(mockLibraryPath))
            KotlinCompilerStandalone(sources, target = File(outDir.path), options = extraOptions, classpath = classpath).compile()

            classLoader = URLClassLoader(
                arrayOf(URL(outDir.url + "/"), mockLibraryJar.toURI().toURL()),
                ForTestCompileRuntime.runtimeJarClassLoader()
            )
        } else {
            val sources = listOf(File(path))
            val outIoDir = File(outDir.path)
            outIoDir.deleteRecursively()
            KotlinCompilerStandalone(sources, target = outIoDir, options = extraOptions).compile()

            classLoader = URLClassLoader(
                arrayOf(URL(outDir.url + "/")),
                ForTestCompileRuntime.runtimeJarClassLoader()
            )
        }

        val stackTraceElement = try {
            val className = JvmFileClassUtil.getFileClassInfoNoResolve(file as KtFile).fileClassFqName
            val clazz = classLoader.loadClass(className.asString())
            clazz.getMethod("box").invoke(null)
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

    override fun tearDown() {
        ConfigLibraryUtil.removeLibrary(module, MOCK_LIBRARY_NAME)
        super.tearDown()
    }

    private fun compileMockLibrary(): File {
        return KotlinCompilerStandalone(listOf(MOCK_LIBRARY_SOURCES)).compile()
    }
}
