/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.script

import com.intellij.codeInsight.highlighting.HighlightUsagesHandler
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestCase
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.exceptionCases.AbstractExceptionCase
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.idea.completion.test.KotlinCompletionTestCase
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionContributor
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager.Companion.updateScriptDependenciesSynchronously
import org.jetbrains.kotlin.idea.core.script.isScriptDependenciesUpdaterDisabled
import org.jetbrains.kotlin.idea.navigation.GotoCheck
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.util.addDependency
import org.jetbrains.kotlin.test.util.projectLibrary
import org.jetbrains.kotlin.test.util.renderAsGotoImplementation
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import org.junit.ComparisonFailure
import java.io.File
import java.util.regex.Pattern
import kotlin.script.dependencies.Environment


abstract class AbstractScriptConfigurationHighlightingTest : AbstractScriptConfigurationTest() {
    fun doTest(path: String) {
        configureScriptFile(path)

        // Highlight references at caret
        HighlightUsagesHandler.invoke(project, editor, myFile)

        checkHighlighting(
            editor,
            InTextDirectivesUtils.isDirectiveDefined(file.text, "// CHECK_WARNINGS"),
            InTextDirectivesUtils.isDirectiveDefined(file.text, "// CHECK_INFOS"))
    }

    fun doComplexTest(path: String) {
        configureScriptFile(path)
        assertException(object : AbstractExceptionCase<ComparisonFailure>() {
            override fun getExpectedExceptionClass(): Class<ComparisonFailure> = ComparisonFailure::class.java

            override fun tryClosure() {
                checkHighlighting(editor, false, false)
            }
        })

        updateScriptDependenciesSynchronously(myFile.virtualFile, project)
        checkHighlighting(editor, false, false)
    }

    override fun setUp() {
        super.setUp()
        ApplicationManager.getApplication().isScriptDependenciesUpdaterDisabled = true
    }

    override fun tearDown() {
        ApplicationManager.getApplication().isScriptDependenciesUpdaterDisabled = false
        super.tearDown()
    }
}

abstract class AbstractScriptConfigurationNavigationTest : AbstractScriptConfigurationTest() {

    fun doTest(path: String) {
        configureScriptFile(path)
        val reference = file!!.findReferenceAt(myEditor.caretModel.offset)!!

        val resolved = reference.resolve()!!.navigationElement!!

        val expectedReference = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// REF:")
        val actualReference = resolved.renderAsGotoImplementation()

        Assert.assertEquals(expectedReference, actualReference)

        val expectedFile = InTextDirectivesUtils.findStringWithPrefixes(file.text, "// FILE:")
        val actualFile = GotoCheck.getFileWithDir(resolved)

        Assert.assertEquals(expectedFile, actualFile)
    }
}

private val validKeys = setOf("sources", "classpath", "imports")
private const val useDefaultTemplate = "// DEPENDENCIES:"
// some bugs can only be reproduced when some module and script have intersecting library dependencies
private const val configureConflictingModule = "// CONFLICTING_MODULE"

private fun String.splitOrEmpty(delimeters: String) = split(delimeters).takeIf { it.size > 1 } ?: emptyList()
internal val switches = listOf(
    useDefaultTemplate,
    configureConflictingModule
)

abstract class AbstractScriptConfigurationTest : KotlinCompletionTestCase() {
    companion object {
        private const val SCRIPT_NAME = "script.kts"
    }

    override fun setUpModule() {
        // do not create default module
    }

    private fun findMainScript(testDir: String) = File(testDir).walkTopDown().find { it.name == SCRIPT_NAME }
            ?: error("Couldn't find $SCRIPT_NAME file in $testDir")

    protected fun configureScriptFile(path: String) {
        val mainScriptFile = findMainScript(path)
        val environment = createScriptEnvironment(mainScriptFile)
        registerScriptTemplateProvider(environment)

        File(path, "mainModule").takeIf { it.exists() }?.let {
            myModule = createTestModuleFromDir(it)
        }

        File(path).listFiles { file -> file.name.startsWith("module") }.filter { it.exists() }.forEach {
            val newModule = createTestModuleFromDir(it)
            assert(myModule != null) { "Main module should exists" }
            ModuleRootModificationUtil.addDependency(myModule, newModule)
        }

        if (configureConflictingModule in environment) {
            val sharedLib = LocalFileSystem.getInstance().findFileByIoFile(environment["lib-classes"] as File)!!
            if (module == null) {
                myModule = createTestModuleByName("mainModule")
            }
            module.addDependency(projectLibrary("sharedLib", classesRoot = sharedLib))
        }

        createFileAndSyncDependencies(mainScriptFile)
    }

    private fun createTestModuleByName(name: String): Module {
        val newModuleDir = runWriteAction { VfsUtil.createDirectoryIfMissing(project.baseDir, name) }
        val newModule = createModuleAtWrapper(name, project, JavaModuleType.getModuleType(), newModuleDir.path)
        PsiTestUtil.addSourceContentToRoots(newModule, newModuleDir)
        return newModule
    }

    private fun createTestModuleFromDir(dir: File): Module {
        return createTestModuleByName(dir.name).apply {
            PlatformTestCase.copyDirContentsTo(LocalFileSystem.getInstance().findFileByIoFile(dir)!!, moduleFile!!.parent)
        }
    }

    private fun createScriptEnvironment(scriptFile: File): Environment {
        val defaultEnvironment = defaultEnvironment(scriptFile.parent + File.separator)
        val env = mutableMapOf<String, Any?>()
        scriptFile.forEachLine { line ->
            line.trim().takeIf { useDefaultTemplate in it }?.substringAfter(useDefaultTemplate)?.split(";")?.forEach { entry ->
                val (key, values) = entry.splitOrEmpty(":").map { it.trim() }
                assert(key in validKeys) { "Unexpected key: $key" }
                env[key] = values.split(",").map {
                    val str = it.trim()
                    defaultEnvironment[str] ?: str
                }
            }

            switches.forEach {
                if (it in line) {
                    env[it] = true
                }
            }
        }
        if (env[useDefaultTemplate] == true && defaultEnvironment["template-classes"] != null) {
            error("Script configuration should be defined by either '$useDefaultTemplate' clause or via template in 'template' directory")
        }
        env.putAll(defaultEnvironment)
        return env
    }

    private fun defaultEnvironment(path: String): Map<String, File?> {
        val templateOutDir = File("${path}template").takeIf { it.isDirectory }?.let {
            compileLibToDir(it, PathUtil.kotlinPathsForDistDirectory.scriptRuntimePath.path)
        }

        val libSrcDir = File("${path}lib").takeIf { it.isDirectory }

        val libClasses = libSrcDir?.let { compileLibToDir(it) }

        return mapOf(
            "runtime-classes" to ForTestCompileRuntime.runtimeJarForTests(),
            "runtime-source" to File("libraries/stdlib/src"),
            "lib-classes" to libClasses,
            "lib-source" to libSrcDir,
            "template-classes" to templateOutDir
        )
    }

    private fun createFileAndSyncDependencies(scriptFile: File) {
        var script: VirtualFile? = null
        if (module != null) {
            script = module.moduleFile?.parent?.findChild(SCRIPT_NAME)
        }

        if (script == null) {
            val target = File(project.basePath, SCRIPT_NAME)
            scriptFile.copyTo(target)
            script = LocalFileSystem.getInstance().findFileByPath(target.path)
        }

        assert(script != null)
        configureByExistingFile(script!!)

        updateScriptDependenciesSynchronously(script, project)
        VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir)
        // This is needed because updateScriptDependencies invalidates psiFile that was stored in myFile field
        myFile = psiManager.findFile(script)
    }

    private fun compileLibToDir(srcDir: File, vararg classpath: String): File {
        val outDir = KotlinTestUtils.tmpDir("${getTestName(false)}${srcDir.name}Out")

        val kotlinSourceFiles = FileUtil.findFilesByMask(Pattern.compile(".+\\.kt$"), srcDir)
        if (kotlinSourceFiles.isNotEmpty()) {
            MockLibraryUtil.compileKotlin(srcDir.path, outDir, extraClasspath = *classpath)
        }

        val javaSourceFiles = FileUtil.findFilesByMask(Pattern.compile(".+\\.java$"), srcDir)
        if (javaSourceFiles.isNotEmpty()) {
            KotlinTestUtils.compileJavaFiles(
                javaSourceFiles,
                listOf("-cp", StringUtil.join(listOf(*classpath, outDir), File.pathSeparator), "-d", outDir.path)
            )
        }
        return outDir
    }

    private fun registerScriptTemplateProvider(environment: Environment) {
        val provider = if (environment[useDefaultTemplate] == true) {
            FromTextTemplateProvider(environment)
        } else {
            CustomScriptTemplateProvider(environment)
        }

        PlatformTestUtil.registerExtension(
            Extensions.getArea(project),
            ScriptDefinitionContributor.EP_NAME,
            provider,
            testRootDisposable
        )
        ScriptDefinitionsManager.getInstance(project).reloadScriptDefinitions()
    }
}
