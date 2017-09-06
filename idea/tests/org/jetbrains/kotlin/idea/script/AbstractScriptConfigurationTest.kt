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

import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.idea.KotlinDaemonAnalyzerTestCase
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager
import org.jetbrains.kotlin.idea.core.script.ScriptDependenciesManager.Companion.updateScriptDependenciesSynchronously
import org.jetbrains.kotlin.idea.navigation.GotoCheck
import org.jetbrains.kotlin.script.ScriptTemplatesProvider
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.util.addDependency
import org.jetbrains.kotlin.test.util.projectLibrary
import org.jetbrains.kotlin.test.util.renderAsGotoImplementation
import org.jetbrains.kotlin.utils.PathUtil
import org.junit.Assert
import java.io.File
import java.util.regex.Pattern
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.dependencies.AsyncDependenciesResolver
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.asSuccess
import kotlin.script.templates.ScriptTemplateDefinition


abstract class AbstractScriptConfigurationHighlightingTest : AbstractScriptConfigurationTest() {
    fun doTest(path: String) {
        configureScriptFile(path)
        checkHighlighting(editor, false, false)
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
private val switches = listOf(
        useDefaultTemplate,
        configureConflictingModule
)

abstract class AbstractScriptConfigurationTest : KotlinDaemonAnalyzerTestCase() {

    protected fun configureScriptFile(path: String) {
        val environment = createScriptEnvironment(path)
        registerScriptTemplateProvider(environment)

        if (configureConflictingModule in environment) {
            val sharedLib = LocalFileSystem.getInstance ().findFileByIoFile(environment["lib-classes"] as File)!!
            module.addDependency(projectLibrary("sharedLib", classesRoot = sharedLib))
        }

        val scriptFile = createFileAndSyncDependencies(path)
        configureByExistingFile(scriptFile)
    }


    private fun createScriptEnvironment(path: String): Environment {
        val defaultEnvironment = defaultEnvironment(path)
        val env = mutableMapOf<String, Any?>()
        File("${path}script.kts").forEachLine { line ->
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

    private fun createFileAndSyncDependencies(path: String): VirtualFile {
        val scriptDir = KotlinTestUtils.tmpDir("scriptDir")
        val target = File(scriptDir, "script.kts")
        File("${path}script.kts").copyTo(target)
        val scriptFile = LocalFileSystem.getInstance().findFileByPath(target.path)!!
        updateScriptDependenciesSynchronously(scriptFile, project)
        return scriptFile
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
        }
        else {
            CustomScriptTemplateProvider(environment)
        }

        PlatformTestUtil.registerExtension(
                Extensions.getArea(project),
                ScriptTemplatesProvider.EP_NAME,
                provider,
                testRootDisposable
        )
        ScriptDependenciesManager.reloadScriptDefinitions(project)
    }
}

class CustomScriptTemplateProvider(
        override val environment: Map<String, Any?>
) : ScriptTemplatesProvider {
    override val id = "Test"
    override val isValid = true
    override val templateClassNames = listOf("custom.scriptDefinition.Template")
    override val templateClasspath = listOfNotNull(environment["template-classes"] as? File)
}

class FromTextTemplateProvider(
        override val environment: Map<String, Any?>
) : ScriptTemplatesProvider {
    override val id = "Test"
    override val isValid = true
    override val templateClassNames = listOf("org.jetbrains.kotlin.idea.script.Template")
    override val templateClasspath get() = emptyList<File>()
}


class FromTextDependenciesResolver : AsyncDependenciesResolver {
    @Suppress("UNCHECKED_CAST")
    suspend override fun resolveAsync(scriptContents: ScriptContents, environment: Environment): DependenciesResolver.ResolveResult {
        return ScriptDependencies(
                classpath = (environment["classpath"] as? List<File>).orEmpty(),
                imports = (environment["imports"] as? List<String>).orEmpty(),
                sources = (environment["sources"] as? List<File>).orEmpty()
        ).asSuccess()
    }
}

@Suppress("unused")
@ScriptTemplateDefinition(FromTextDependenciesResolver::class, scriptFilePattern = "script.kts")
class Template