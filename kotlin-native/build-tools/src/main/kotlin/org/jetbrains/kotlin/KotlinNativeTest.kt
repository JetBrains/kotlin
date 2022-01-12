/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */
package org.jetbrains.kotlin

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.kotlin.dsl.getByType
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.process.ExecSpec
import org.jetbrains.kotlin.konan.exec.Command
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.LinkerOutputKind
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.regex.Pattern

abstract class KonanTest : DefaultTask(), KonanTestExecutable {
    enum class Logger {
        EMPTY,    // Built without test runner
        GTEST,    // Google test log output
        TEAMCITY, // TeamCity log output
        SIMPLE,   // Prints simple messages of passed/failed tests
        SILENT    // Prints no log of passed/failed tests
    }

    @get:Input
    var disabled: Boolean
        get() = !enabled
        set(value) {
            enabled = !value
        }

    /**
     * Test output directory. Used to store processed sources and binary artifacts.
     */
    @get:OutputDirectory
    abstract val outputDirectory: String

    /**
     * Test logger to be used for the test built with TestRunner (`-tr` option).
     */
    @get:Internal
    abstract var testLogger: Logger

    /**
     * Test executable arguments.
     */
    @Input
    var arguments = mutableListOf<String>()

    /**
     * Test executable.
     */
    abstract override val executable: String

    /**
     * Test source.
     */
    @Internal
    lateinit var source: String

    /**
     * Sets test filtering to choose the exact test in the executable built with TestRunner.
     */
    @Input
    var useFilter = true

    /**
     * An action to be executed before the build.
     * As this run task comes after the build task all actions for doFirst
     * should be done before the build and not run.
     */
    @Internal
    override var doBeforeBuild: Action<in Task>? = null

    @Internal
    override var doBeforeRun: Action<in Task>? = null

    @get:Internal
    override val buildTasks: List<Task>
        get() = listOf(project.findKonanBuildTask(name, project.testTarget).get())

    @Suppress("UnstableApiUsage")
    override fun configure(config: Closure<*>): Task {
        super.configure(config)

        // Set Gradle properties for the better navigation
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Kotlin/Native test infrastructure task"

        if (testLogger != Logger.EMPTY) {
            arguments.add("--ktest_logger=$testLogger")
        }
        if (useFilter && ::source.isInitialized) {
            arguments.add("--ktest_filter=${source.convertToPattern()}")
        }
        this.dependsOnDist()
        return this
    }

    @TaskAction
    open fun run() = project.executeAndCheck(project.file(executable).toPath(), arguments)

    // Converts to runner's pattern
    private fun String.convertToPattern() = this.removeSuffix(".kt").replace("/", ".") + ".*"

    internal fun ProcessOutput.print(prepend: String = "") {
        if (project.verboseTest)
            println(prepend + """
                |stdout:
                |$stdOut
                |stderr:
                |$stdErr
                |exit code: $exitCode
                """.trimMargin())
    }
}

/**
 * Create a test task of the given type. Supports configuration with Closure passed form build.gradle file.
 */
fun <T : KonanTestExecutable> Project.createTest(name: String, type: Class<T>, config: Closure<*>): T =
        project.tasks.create(name, type).apply {
            // Apply closure set in build.gradle to get all parameters.
            this.configure(config)
            if (enabled) {
                // If run task depends on something, build tasks should also depend on this.
                buildTasks.forEach { buildTask ->
                    buildTask.sameDependenciesAs(this)
                    // Run task should depend on compile task
                    this.dependsOn(buildTask)
                    doBeforeBuild?.let { buildTask.doFirst(it) }
                    buildTask.enabled = enabled
                }
            }
        }

/**
 * Task to run tests compiled with TestRunner.
 * Runs tests with GTEST output and parses it to create statistics info
 */
open class KonanGTest : KonanTest() {
    override val outputDirectory = "${project.testOutputStdlib}/$name"

    // Use GTEST logger to parse test results later
    override var testLogger = Logger.GTEST

    @get:Internal
    override val executable: String
        get() = "$outputDirectory/${project.testTarget.name}/$name.${project.testTarget.family.exeSuffix}"

    @Internal
    var statistics = Statistics()

    @TaskAction
    override fun run() {
        doBeforeRun?.execute(this)
        if (project.compileOnlyTests) {
            return
        }
        runProcess(
                executor = { project.executor.execute(it) },
                executable = executable,
                args = arguments
        ).run {
            parse(stdOut)
            println("""
                |stdout:
                |$stdOut
                |stderr:
                |$stdErr
                |exit code: $exitCode
                """.trimMargin())
            check(exitCode == 0) { "Test $executable exited with $exitCode" }
        }
    }

    private fun parse(output: String) = statistics.apply {
        Pattern.compile("\\[  PASSED  ] ([0-9]*) tests\\.").matcher(output)
                .apply { if (find()) pass(group(1).toInt()) }

        Pattern.compile("\\[  FAILED  ] ([0-9]*) tests.*").matcher(output)
                .apply { if (find()) fail(group(1).toInt()) }

        Pattern.compile("\\[  SKIPPED ] ([0-9]*) test.*").matcher(output)
                .apply { if (find()) skip(group(1).toInt()) }
        if (total == 0) {
            // No test were run. Try to find if we've tried to run something
            error(Pattern.compile("\\[={10}] Running ([0-9]*) tests from ([0-9]*) test cases\\..*")
                    .matcher(output)
                    .run { if (find()) group(1).toInt() else 1 })
        }
    }
}

/**
 * Task to run tests built into a single predefined binary named `localTest`.
 * Note: this task should depend on task that builds a test binary.
 */
open class KonanLocalTest : KonanTest() {
    override val outputDirectory = project.testOutputLocal

    // local tests built into a single binary with the known name
    @get:Internal
    override val executable: String
        get() = "$outputDirectory/${project.testTarget.name}/localTest.${project.testTarget.family.exeSuffix}"

    override var testLogger = Logger.SILENT

    @Input
    @Optional
    var expectedExitStatus: Int? = null

    @Internal
    var expectedExitStatusChecker: (Int) -> Boolean = { it == (expectedExitStatus ?: 0) }

    /**
     * Should this test fail or not.
     */
    @Input
    var expectedFail = false

    /**
     * Used to validate output against the golden data.
     */
    @Input
    var useGoldenData: Boolean = false

    @get:InputFile
    @get:Optional
    open val goldenDataFile: File?
        get() {
            val goldenDataFile = computeGoldenDataFile()
            return if (useGoldenData) {
                check(goldenDataFile.isFile) { "Task $name. Golden data file does not exist: $goldenDataFile" }
                goldenDataFile
            } else {
                check(!goldenDataFile.exists()) { "Task $name. Golden data file should not exist: $goldenDataFile" }
                null
            }
        }

    protected open fun computeGoldenDataFile(): File {
        val sourceFile = project.file(source)
        return sourceFile.parentFile.resolve(sourceFile.nameWithoutExtension + ".out")
    }

    private val goldenData: String?
        get() = goldenDataFile?.readText(Charsets.UTF_8)

    /**
     * Checks test's output against gold value and returns true if the output matches the expectation.
     */
    @Internal
    var outputChecker: (String) -> Boolean = { output ->
        if (useGoldenData) goldenData == output else true
    }

    /**
     * Input test data to be passed to process stdin.
     */
    @Input
    var useTestData: Boolean = false

    @get:InputFile
    @get:Optional
    val testDataFile: File?
        get() {
            val sourceFile = project.file(source)
            val testDataFile = sourceFile.parentFile.resolve(sourceFile.nameWithoutExtension + ".in")
            return if (useTestData) {
                check(testDataFile.isFile) { "Task $name. Test data file does not exist: $testDataFile" }
                testDataFile
            } else {
                check(!testDataFile.exists()) { "Task $name. Test data file should not exist: $testDataFile" }
                null
            }
        }

    private val testData: String?
        get() = testDataFile?.readText(Charsets.UTF_8)

    /**
     * Should compiler message be read and validated with output checker or gold value.
     */
    @Input
    var compilerMessages = false

    @Input
    var multiRuns = false

    @Input
    @Optional
    var multiArguments: List<List<String>>? = null

    @TaskAction
    override fun run() {
        doBeforeRun?.execute(this)
        if (project.compileOnlyTests) {
            return
        }
        val times = if (multiRuns && multiArguments != null) multiArguments!!.size else 1
        var output = ProcessOutput("", "", 0)
        for (i in 1..times) {
            val args = arguments + (multiArguments?.get(i - 1) ?: emptyList())
            val testData = this.testData
            output += if (testData != null)
                runProcessWithInput({ project.executor.execute(it) }, executable, args, testData)
            else
                runProcess({ project.executor.execute(it) }, executable, args)
        }
        if (compilerMessages) {
            // TODO: as for now it captures output only in the driver task.
            // It should capture output from the build task using Gradle's LoggerManager and LoggerOutput
            val compilationLog = project.file("$executable.compilation.log").readText()
            output.stdOut = compilationLog + output.stdOut
        }
        output.check()
        output.print()
    }

    private operator fun ProcessOutput.plus(other: ProcessOutput) = ProcessOutput(
            stdOut + other.stdOut,
            stdErr + other.stdErr,
            exitCode + other.exitCode)

    private fun ProcessOutput.check() {
        val exitCodeMismatch = !expectedExitStatusChecker(exitCode)
        if (exitCodeMismatch) {
            val message = if (expectedExitStatus != null)
                "Expected exit status: $expectedExitStatus, actual: $exitCode"
            else
                "Actual exit status doesn't match with exit status checker: $exitCode"
            check(expectedFail) {
                """
                    |Test failed. $message
                    |stdout:
                    |$stdOut
                    |stderr:
                    |$stdErr
                    """.trimMargin()
            }
            println("Expected failure. $message")
        }

        val output = stdOut + stdErr
        val outputMismatch = !outputChecker(output.replace(System.lineSeparator(), "\n"))
        if (outputMismatch) {
            val message = goldenData?.let { goldenData -> "Expected output: $goldenData, actual output: $output" }
                    ?: "Actual output doesn't match with output checker: $output"

            check(expectedFail) { "Test failed. $message" }
            println("Expected failure. $message")
        }

        check((exitCodeMismatch || outputMismatch) || !expectedFail) {
            """
            |Unexpected pass:
            | * exit code mismatch: $exitCodeMismatch
            | * gold value mismatch: $outputMismatch
            | * expected fail: $expectedFail
            """.trimMargin()
        }
    }
}

/**
 * Executes a standalone tests provided with either @param executable or by the tasks @param name.
 * The executable itself should be built by the konan plugin.
 */
open class KonanStandaloneTest : KonanLocalTest() {
    init {
        useFilter = false
    }

    override val outputDirectory: String
        get() = "${project.testOutputLocal}/$name"

    override var testLogger = Logger.EMPTY

    override val executable: String
        get() = "$outputDirectory/${project.testTarget.name}/$name.${project.testTarget.family.exeSuffix}"

    @Input
    var enableKonanAssertions = true

    @Input
    var verifyIr = true

    /**
     * Compiler flags used to build a test.
     */
    @Internal
    var flags: List<String> = listOf()
        get() {
            val result = field.toMutableList()
            if (enableKonanAssertions)
                result += "-ea"
            if (verifyIr)
                result += "-Xverify-ir"
            return result
        }

    @Internal
    fun getSources(): Provider<List<String>> = project.provider {
        val sources = buildCompileList(project.file(source).toPath(), outputDirectory)
        sources.forEach { it.writeTextToFile() }
        sources.map { it.path }
    }
}

/**
 * This is another way to run the konanc compiler. It runs a konanc shell script.
 *
 * @note This task is not intended for regular testing as project.exec + a shell script isolate the jvm from IDEA.
 * @see KonanLocalTest to be used as a regular task.
 */
open class KonanDriverTest : KonanStandaloneTest() {
    override fun configure(config: Closure<*>): Task {
        super.configure(config)
        doFirst { konan() }
        doBeforeBuild?.let { doFirst(it) }
        return this
    }

    private fun konan() {
        val dist = project.kotlinNativeDist
        val konancDriver = if (HostManager.hostIsMingw) "konanc.bat" else "konanc"
        val konanc = File("${dist.canonicalPath}/bin/$konancDriver").absolutePath

        File(executable).parentFile.mkdirs()

        val args = mutableListOf("-output", executable).apply {
            if (project.testTarget != HostManager.host) {
                add("-target")
                add(project.testTarget.visibleName)
            }
            addAll(getSources().get())
            addAll(flags)
            addAll(project.globalTestArgs)
        }

        // run konanc compiler locally
        runProcess(localExecutor(project), konanc, args).let {
            it.print("Konanc compiler execution:")
            project.file("$executable.compilation.log").run {
                writeText(it.stdOut)
                appendText(it.stdErr)
            }
            check(it.exitCode == 0) {
                "Compiler failed with exit code ${it.exitCode}\n" +
                        "stdOut: ${it.stdOut}\n" +
                        "stdErr: ${it.stdErr}"
            }
        }
    }
}

open class KonanInteropTest : KonanStandaloneTest() {
    /**
     * Name of the interop library
     */
    @Input
    lateinit var interop: String
}

open class KonanLinkTest : KonanStandaloneTest() {
    @Input
    lateinit var lib: String
}

/**
 * Test task to check a library built by `-produce dynamic`.
 * C source code should contain `testlib` as a reference to a testing library.
 * It will be replaced then by the actual library name.
 */
open class KonanDynamicTest : KonanStandaloneTest() {
    override fun configure(config: Closure<*>): Task {
        super.configure(config)
        doFirst { clang() }
        return this
    }

    /**
     * File path to the C source.
     */
    @get:Input
    lateinit var cSource: String

    @Input
    var clangTool = "clang"

    @Input
    var clangFlags: List<String> = listOf()

    @Input
    @Optional
    var interop: String? = null

    override fun computeGoldenDataFile(): File {
        val sourceFile = project.file(source)
        val cSourceFile = File(cSource)
        return sourceFile.parentFile.resolve(sourceFile.nameWithoutExtension + "-" + cSourceFile.nameWithoutExtension + ".out")
    }

    // Replace testlib_api.h and all occurrences of the testlib with the actual name of the test
    private fun processCSource(): String {
        val sourceFile = File(cSource)
        val prefixedName = if (HostManager.hostIsMingw) name else "lib$name"
        val res = sourceFile.readText()
                .replace("#include \"testlib_api.h\"", "#include \"${prefixedName}_api.h\"")
                .replace("testlib", prefixedName)
        val newFileName = "$outputDirectory/${sourceFile.name}"
        println(newFileName)
        File(newFileName).run {
            createNewFile()
            writeText(res)
        }
        return newFileName
    }

    private fun clang() {
        val log = ByteArrayOutputStream()
        val plugin = project.extensions.getByType<ExecClang>()
        val artifactsDir = "$outputDirectory/${project.testTarget}"

        fun flagsContain(opt: String) = project.globalTestArgs.contains(opt) || flags.contains(opt)
        val isOpt = flagsContain("-opt")
        val isDebug = flagsContain("-g")

        val execResult = plugin.execKonanClang(project.testTarget, Action<ExecSpec> {
            workingDir = File(outputDirectory)
            this@Action.executable = clangTool
            args = listOf(processCSource(),
                    "-c",
                    "-o", "${this@KonanDynamicTest.executable}.o",
                    "-I", artifactsDir
            ) + clangFlags
            standardOutput = log
            errorOutput = log
            isIgnoreExitValue = false
        })
        log.toString("UTF-8").also {
            project.file("$executable.compilation.log").writeText(it)
            println(it)
        }
        execResult.assertNormalExitValue()

        val linker = project.platformManager.platform(project.testTarget).linker
        val linkerArgs = when (project.testTarget.family) {
            // rpath is meaningless on Windows (and isn't supported by LLD).
            // --allow-multiple-definition is needed because finalLinkCommands statically links a lot of MinGW-specific libraries,
            // that are already included in DLL produced by Kotlin/Native.
            Family.MINGW -> listOf("-L", artifactsDir, "-Wl,--allow-multiple-definition")
            else -> listOf("-L", artifactsDir, "-rpath", artifactsDir)
        }
        val commands = linker.finalLinkCommands(
                objectFiles = listOf("${this@KonanDynamicTest.executable}.o"),
                executable = executable,
                libraries = listOf("-l$name"),
                linkerArgs = linkerArgs,
                optimize = isOpt,
                debug = isDebug,
                kind = LinkerOutputKind.EXECUTABLE,
                outputDsymBundle = "",
                needsProfileLibrary = false,
                mimallocEnabled = false
        )
        commands.map { cmd ->
            // Filter out linker option that defines __cxa_demangle because Konan_cxa_demangle is not defined in tests.
            Command(cmd.argsWithExecutable.filterNot { it.contains("--defsym") || it.contains("Konan_cxa_demangle") })
        }.forEach {
            it.logWith { message -> project.file("$executable.compilation.log").appendText(message()) }
            it.execute()
        }
    }
}
