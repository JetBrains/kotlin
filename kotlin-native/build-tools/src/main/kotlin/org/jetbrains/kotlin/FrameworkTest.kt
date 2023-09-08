package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Task
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin

import org.jetbrains.kotlin.konan.target.*
import java.io.File
import java.io.FileWriter
import java.io.Serializable
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Test task for -produce framework testing. Requires a framework to be built by the Konan plugin
 * with konanArtifacts { framework(frameworkName, targets: [ testTarget] ) } and a dependency set
 * according to a pattern "compileKonan${frameworkName}".
 *
 * @property swiftSources  Swift-language test sources that use a given framework
 * @property frameworks names of frameworks
 */
open class FrameworkTest : DefaultTask(), KonanTestExecutable {
    @Input
    lateinit var swiftSources: List<String>

    @Input
    var swiftExtraOpts: List<String> = emptyList()

    @Input
    lateinit var frameworks: MutableList<Framework>

    @Input
    var fullBitcode: Boolean = false

    @Input
    var codesign: Boolean = true

    @Input
    val testOutput: String = project.testOutputFramework

    @Input @Optional
    var expectedExitStatus: Int? = null

    /**
     * Framework description.
     *
     * @param name is the framework name,
     * @param sources framework sources,
     * @param bitcode bitcode embedding in the framework,
     * @param isStatic determines that framework is static
     * @param artifact the name of the resulting artifact,
     * @param library list of library dependency names,
     * @param opts additional options for the compiler.
     */
    class Framework(
            val name: String,
            var sources: List<String> = emptyList(),
            var bitcode: Boolean = false,
            var isStatic: Boolean = false,
            var artifact: String = name,
            var libraries: List<String> = emptyList(),
            var opts: List<String> = emptyList()
    ) : Serializable // Required for Gradle when using Framework as task input.

    /**
     * Used for the framework configuration in the task's closure.
     */
    fun framework(name: String, closure: Closure<Framework>): Framework {
        val f = Framework(name).apply {
            closure.delegate = this
            closure.resolveStrategy = Closure.DELEGATE_FIRST
            closure.call()
            // map to file paths
            sources = sources.toFiles(Language.Kotlin).map { it.path }
        }
        if (!::frameworks.isInitialized) {
            frameworks = mutableListOf(f)
        } else {
            frameworks.add(f)
        }
        return f
    }

    enum class Language(val extension: String) {
        Kotlin(".kt"), ObjC(".m"), Swift(".swift")
    }

    fun Language.filesFrom(dir: String): FileTree = project.fileTree(dir) {
        // include only files with the language extension
        include("*${this@filesFrom.extension}")
    }

    fun List<String>.toFiles(language: Language): List<File> =
            this.map { language.filesFrom(it) }
                    .flatMap { it.files }

    @get:Internal
    override val executable: String
        get() = Paths.get(testOutput, name, "swiftTestExecutable").toString()

    @Internal
    override var doBeforeRun: Action<in Task>? = null

    @Internal
    override var doBeforeBuild: Action<in Task>? = null

    @get:Internal
    override val buildTasks: List<Task>
        get() = frameworks.map { project.tasks.getByName("compileKonan${it.name}") }

    @Suppress("UnstableApiUsage")
    override fun configure(config: Closure<*>): Task {
        super.configure(config)
        // set crossdist build dependency if custom konan.home wasn't set
        this.dependsOnDist()

        // Set Gradle properties for the better navigation
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Kotlin/Native test infrastructure task"

        check(::frameworks.isInitialized) { "Frameworks should be set" }
        return this
    }

    private fun buildTestExecutable() {
        val frameworkParentDirPath = "$testOutput/$name/${project.testTarget.name}"
        frameworks.forEach { framework ->
            val frameworkArtifact = framework.artifact
            val frameworkPath = "$frameworkParentDirPath/$frameworkArtifact.framework"
            if (codesign) codesign(project, frameworkPath)
        }

        // create a test provider and get main entry point
        val provider = Paths.get(testOutput, name, "provider.swift")
        FileWriter(provider.toFile()).use { writer ->
            val providers = swiftSources.toFiles(Language.Swift)
                    .map { file ->
                        file.name.toString().removeSuffix(".swift").replaceFirstChar { it.uppercase() }
                    }
                    .map { "${it}Tests" }

            writer.write("""
                |// THIS IS AUTOGENERATED FILE
                |// This method is invoked by the main routine to get a list of tests
                |func registerProviders() {
                |    ${providers.joinToString("\n    ") { "$it()" }}
                |}
                """.trimMargin())
        }
        val testHome = project.file("framework").toPath()
        val swiftMain = Paths.get(testHome.toString(), "main.swift").toString()

        // Compile swift sources
        val sources = swiftSources.toFiles(Language.Swift)
                .map { it.path } + listOf(provider.toString(), swiftMain)
        val options = listOf(
                "-g",
                "-Xlinker", "-rpath", "-Xlinker", "@executable_path/Frameworks",
                "-Xlinker", "-rpath", "-Xlinker", frameworkParentDirPath,
                "-F", frameworkParentDirPath,
                "-Xcc", "-Werror" // To fail compilation on warnings in framework header.
        )
        // As of Xcode 13.1 swift passes wrong libclang_rt to simulator targets (similar to KT-47333).
        // To workaround this problem, we explicitly provide the correct one.
        val simulatorHack = if (project.testTargetConfigurables.targetTriple.isSimulator) {
            project.platformManager.platform(project.testTarget).linker.provideCompilerRtLibrary("")?.let {
                listOf("-Xlinker", it)
            } ?: emptyList()
        } else {
            emptyList()
        }
        compileSwift(project, project.testTarget, sources, options + simulatorHack + swiftExtraOpts, Paths.get(executable), fullBitcode)
    }

    @TaskAction
    fun run() {
        // Build test executable as a first action of the task before executing the test
        buildTestExecutable()
        doBeforeRun?.execute(this)
        val testExecutable = Paths.get(executable)
        val (stdOut, stdErr, exitCode) = runProcess(
                executor = {
                    project.executor.execute {
                        it.execute(this)
                        workingDir = Paths.get(testOutput).toFile()
                    }
                },
                executable = testExecutable.toString())

        val testExecName = testExecutable.fileName
        println("""
            |$testExecName
            |stdout: $stdOut
            |stderr: $stdErr
            """.trimMargin())
        val timeoutMessage = if (exitCode == -1) {
            "WARNING: probably a timeout\n"
        } else ""
        check(exitCode == (expectedExitStatus ?: 0)) { "${timeoutMessage}Execution of $testExecName failed with exit code: $exitCode " }
    }
}
