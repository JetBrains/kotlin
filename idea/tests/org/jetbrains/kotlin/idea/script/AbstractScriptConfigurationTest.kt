/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.script

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PlatformTestCase
import com.intellij.testFramework.PsiTestUtil
import com.intellij.util.ui.UIUtil
import org.jdom.Element
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.idea.completion.test.KotlinCompletionTestCase
import org.jetbrains.kotlin.idea.core.script.IdeScriptReportSink
import org.jetbrains.kotlin.idea.core.script.ScriptConfigurationManager.Companion.updateScriptDependenciesSynchronously
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionContributor
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionsManager
import org.jetbrains.kotlin.idea.core.script.isScriptChangesNotifierDisabled
import org.jetbrains.kotlin.idea.core.script.settings.KotlinScriptingSettings
import org.jetbrains.kotlin.idea.highlighter.KotlinHighlightingUtil
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.idea.util.getProjectJdkTableSafe
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.scripting.definitions.findScriptDefinition
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.addDependency
import org.jetbrains.kotlin.test.util.projectLibrary
import org.jetbrains.kotlin.utils.PathUtil
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_JAVA_SCRIPT_RUNTIME_JAR
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_SCRIPTING_COMMON_JAR
import org.jetbrains.kotlin.utils.PathUtil.KOTLIN_SCRIPTING_JVM_JAR
import java.io.File
import java.util.regex.Pattern
import kotlin.script.dependencies.Environment
import kotlin.script.experimental.api.ScriptDiagnostic

private val validKeys = setOf("javaHome", "sources", "classpath", "imports", "template-classes-names")
private const val useDefaultTemplate = "// DEPENDENCIES:"
private const val templatesSettings = "// TEMPLATES: "

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

    private fun findMainScript(testDir: String): File {
        val scriptFile = File(testDir).walkTopDown().find { it.name == SCRIPT_NAME }
        if (scriptFile != null) return scriptFile

        return File(testDir).walkTopDown().singleOrNull { it.name.contains("script") }
            ?: error("Couldn't find $SCRIPT_NAME file in $testDir")
    }

    private val sdk by lazy {
        val jdk = PluginTestCaseBase.jdk(TestJdkKind.MOCK_JDK)
        runWriteAction {
            getProjectJdkTableSafe().addJdk(jdk, testRootDisposable)
            ProjectRootManager.getInstance(project).projectSdk = jdk
        }
        jdk
    }

    protected fun configureScriptFile(path: String) {
        val mainScriptFile = findMainScript(path)
        configureScriptFile(path, mainScriptFile)
    }

    protected fun configureScriptFile(path: String, mainScriptFile: File) {
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

        if (module != null) {
            module.addDependency(
                projectLibrary(
                    "script-runtime",
                    classesRoot = VfsUtil.findFileByIoFile(PathUtil.kotlinPathsForDistDirectory.scriptRuntimePath, true)
                )
            )

            if (environment["template-classes"] != null) {
                module.addDependency(
                    projectLibrary(
                        "script-template-library",
                        classesRoot = VfsUtil.findFileByIoFile(environment["template-classes"] as File, true)
                    )
                )
            }
        }

        if (configureConflictingModule in environment) {
            val sharedLib = VfsUtil.findFileByIoFile(environment["lib-classes"] as File, true)!!
            if (module == null) {
                // Force create module if it doesn't exist
                myModule = createTestModuleByName("mainModule")
            }
            module.addDependency(projectLibrary("sharedLib", classesRoot = sharedLib))
        }

        if (module != null) {
            ModuleRootModificationUtil.updateModel(module) { model ->
                model.sdk = sdk
            }
        }

        createFileAndSyncDependencies(mainScriptFile)
    }

    private val oldScripClasspath: String? = System.getProperty("kotlin.script.classpath")

    private var settings: Element? = null

    override fun setUp() {
        super.setUp()
        ApplicationManager.getApplication().isScriptChangesNotifierDisabled = true

        settings = KotlinScriptingSettings.getInstance(project).state

        ScriptDefinitionsManager.getInstance(project).getAllDefinitions().forEach {
            KotlinScriptingSettings.getInstance(project).setEnabled(it, false)
        }

        setUpTestProject()
    }

    open fun setUpTestProject() {

    }

    override fun tearDown() {
        ApplicationManager.getApplication().isScriptChangesNotifierDisabled = false

        System.setProperty("kotlin.script.classpath", oldScripClasspath ?: "")

        settings?.let {
            KotlinScriptingSettings.getInstance(project).loadState(it)
        }

        super.tearDown()
    }

    override fun getTestProjectJdk(): Sdk {
        return PluginTestCaseBase.mockJdk()
    }

    private fun createTestModuleByName(name: String): Module {
        val newModuleDir = runWriteAction { VfsUtil.createDirectoryIfMissing(project.baseDir, name) }
        val newModule = createModuleAt(name, project, JavaModuleType.getModuleType(), newModuleDir.path)

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

            fun iterateKeysInLine(prefix: String) {
                if (line.contains(prefix)) {
                    line.trim().substringAfter(prefix).split(";").forEach { entry ->
                        val (key, values) = entry.splitOrEmpty(":").map { it.trim() }
                        assert(key in validKeys) { "Unexpected key: $key" }
                        env[key] = values.split(",").map {
                            val str = it.trim()
                            defaultEnvironment[str] ?: str
                        }
                    }
                }
            }

            iterateKeysInLine(useDefaultTemplate)
            iterateKeysInLine(templatesSettings)

            switches.forEach {
                if (it in line) {
                    env[it] = true
                }
            }
        }

        if (env[useDefaultTemplate] != true && env["template-classes-names"] == null) {
            env["template-classes-names"] = listOf("custom.scriptDefinition.Template")
        }

        val jdkKind = when ((env["javaHome"] as? List<String>)?.singleOrNull()) {
            "9" -> TestJdkKind.FULL_JDK_9
            else -> TestJdkKind.MOCK_JDK
        }
        runWriteAction {
            val jdk = PluginTestCaseBase.jdk(jdkKind)
            getProjectJdkTableSafe().addJdk(jdk, testRootDisposable)
            env["javaHome"] = File(jdk.homePath)
        }

        env.putAll(defaultEnvironment)
        return env
    }

    private fun defaultEnvironment(path: String): Map<String, File?> {
        val templateOutDir = File("${path}template").takeIf { it.isDirectory }?.let {
            compileLibToDir(it, *scriptClasspath())
        } ?: File("idea/testData/script/definition/defaultTemplate").takeIf { it.isDirectory }?.let {
            compileLibToDir(it, *scriptClasspath())
        }

        if (templateOutDir != null) {
            System.setProperty("kotlin.script.classpath", templateOutDir.path)
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

    private fun scriptClasspath(): Array<String> {
        return with(PathUtil.kotlinPathsForDistDirectory) {
            arrayOf(
                File(libPath, KOTLIN_JAVA_SCRIPT_RUNTIME_JAR).path,
                File(libPath, KOTLIN_SCRIPTING_COMMON_JAR).path,
                File(libPath, KOTLIN_SCRIPTING_JVM_JAR).path
            )
        }
    }

    protected fun createFileAndSyncDependencies(scriptFile: File) {
        var script: VirtualFile? = null
        if (module != null) {
            script = module.moduleFile?.parent?.findChild(scriptFile.name)
        }

        if (script == null) {
            val target = File(project.basePath, scriptFile.name)
            scriptFile.copyTo(target)
            script = VfsUtil.findFileByIoFile(target, true)
        }

        if (script == null) error("Test file with script couldn't be found in test project")

        configureByExistingFile(script)
        loadScriptConfigurationSynchronously(script)
    }

    protected open fun loadScriptConfigurationSynchronously(script: VirtualFile) {
        updateScriptDependenciesSynchronously(myFile, project)

        // This is needed because updateScriptDependencies invalidates psiFile that was stored in myFile field
        VfsUtil.markDirtyAndRefresh(false, true, true, project.baseDir)
        myFile = psiManager.findFile(script)

        checkHighlighting()
    }

    protected fun checkHighlighting(file: KtFile = myFile as KtFile) {
        val reports = IdeScriptReportSink.getReports(file)
        val isFatalErrorPresent = reports.any { it.severity == ScriptDiagnostic.Severity.FATAL }
        assert(isFatalErrorPresent || KotlinHighlightingUtil.shouldHighlight(file)) {
            "Highlighting is switched off for ${file.virtualFile.path}\n" +
                    "reports=$reports\n" +
                    "scriptDefinition=${file.findScriptDefinition()}"
        }
    }

    private fun compileLibToDir(srcDir: File, vararg classpath: String): File {
        //TODO: tmpDir would be enough, but there is tricky fail under AS otherwise
        val outDir = KotlinTestUtils.tmpDirForReusableFolder("${getTestName(false)}${srcDir.name}Out")

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

        addExtensionPointInTest(
            ScriptDefinitionContributor.EP_NAME,
            project,
            provider,
            testRootDisposable
        )

        ScriptDefinitionsManager.getInstance(project).reloadScriptDefinitions()

        UIUtil.dispatchAllInvocationEvents()
    }
}
