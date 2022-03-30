/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import com.intellij.testFramework.TestDataFile
import org.gradle.api.logging.LogLevel
import org.gradle.api.logging.configuration.WarningMode
import org.gradle.tooling.GradleConnector
import org.gradle.util.GradleVersion
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.common.CompilerSystemProperties.COMPILE_INCREMENTAL_WITH_ARTIFACT_TRANSFORM
import org.jetbrains.kotlin.gradle.model.ModelContainer
import org.jetbrains.kotlin.gradle.model.ModelFetcherBuildAction
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.report.BuildReportType
import org.jetbrains.kotlin.gradle.testbase.*
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.gradle.util.modify
import org.jetbrains.kotlin.konan.target.HostManager
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.test.RunnerWithMuteInDatabase
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import org.junit.runner.RunWith
import java.io.File
import java.nio.file.Files
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.io.path.isDirectory
import kotlin.test.*

val SYSTEM_LINE_SEPARATOR: String = System.getProperty("line.separator")

@RunWith(value = RunnerWithMuteInDatabase::class)
abstract class BaseGradleIT {

    protected var workingDir = File(".")

    internal open fun defaultBuildOptions(): BuildOptions = BuildOptions(
        withDaemon = true,
        enableKpmModelMapping = isKpmModelMappingEnabled
    )

    open val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.None

    val isTeamCityRun = System.getenv("TEAMCITY_VERSION") != null

    /**
     * `var` makes it configurable per test
     * `open` makes it configurable per test suite
     */
    protected open var isKpmModelMappingEnabled = System
        .getProperty("kotlin.gradle.kpm.enableModelMapping")
        .toBoolean()

    @Before
    open fun setUp() {
        // Aapt2 from Android Gradle Plugin 3.2 and below does not handle long paths on Windows.
        workingDir = createTempDir(if (isWindows) "" else "BaseGradleIT")
        defaultBuildOptions().androidHome?.let { acceptAndroidSdkLicenses(it) }
    }

    @After
    fun tearDown() {
        workingDir.deleteRecursively()
    }


    companion object {
        // https://developer.android.com/studio/intro/update.html#download-with-gradle
        fun acceptAndroidSdkLicenses(androidHome: File) {
            val sdkLicensesDir = androidHome.resolve("licenses")
            if (!sdkLicensesDir.exists()) sdkLicensesDir.mkdirs()

            val sdkLicenses = listOf(
                "8933bad161af4178b1185d1a37fbf41ea5269c55",
                "d56f5187479451eabf01fb78af6dfcb131a6481e",
                "24333f8a63b6825ea9c5514f83c2829b004d1fee",
            )
            val sdkPreviewLicense = "84831b9409646a918e30573bab4c9c91346d8abd"

            val sdkLicenseFile = sdkLicensesDir.resolve("android-sdk-license")
            if (!sdkLicenseFile.exists()) {
                sdkLicenseFile.createNewFile()
                sdkLicenseFile.writeText(
                    sdkLicenses.joinToString(separator = "\n")
                )
            } else {
                sdkLicenses
                    .subtract(
                        sdkLicenseFile.readText().lines()
                    )
                    .forEach {
                        sdkLicenseFile.appendText("$it\n")
                    }
            }

            val sdkPreviewLicenseFile = sdkLicensesDir.resolve("android-sdk-preview-license")
            if (!sdkPreviewLicenseFile.exists()) {
                sdkPreviewLicenseFile.writeText(sdkPreviewLicense)
            } else {
                if (sdkPreviewLicense != sdkPreviewLicenseFile.readText().trim()) {
                    sdkPreviewLicenseFile.writeText(sdkPreviewLicense)
                }
            }
        }

        private object DaemonRegistry {
            // wrapper version to the number of daemon runs performed
            private val daemonRunCount = hashMapOf<String, Int>()
            private val runnerGradleVersion = System.getProperty("runnerGradleVersion")

            val activeDaemons: List<String>
                get() = daemonRunCount.keys.toList()

            fun register(version: String) {
                if (version == runnerGradleVersion) return

                daemonRunCount[version] = (daemonRunCount[version] ?: 0) + 1
            }

            fun unregister(version: String) {
                daemonRunCount.remove(version)
            }

            fun runCountForDaemon(version: String): Int =
                daemonRunCount[version] ?: 0
        }


        // gradle wrapper version to wrapper directory
        private val gradleWrappers = hashMapOf<String, File>()
        private const val MAX_DAEMON_RUNS = 100
        private const val MAX_ACTIVE_GRADLE_PROCESSES = 1

        private fun getEnvJDK_18() = System.getenv()["JDK_18"]

        val resourcesRootFile = File("src/test/resources")

        @AfterClass
        @JvmStatic
        @Synchronized
        @Suppress("unused")
        fun tearDownAll() {
            // Latest gradle requires Java > 7
            val environmentVariables = hashMapOf<String, String>()
            getEnvJDK_18()?.let { environmentVariables["JAVA_HOME"] = it }
            stopAllDaemons(environmentVariables)

            gradleWrappers.values.forEach { wrapperDir ->
                wrapperDir.deleteRecursively()
            }
            gradleWrappers.clear()
        }

        @Synchronized
        fun prepareWrapper(
            version: String,
            environmentVariables: Map<String, String> = mapOf(),
            withDaemon: Boolean = true
        ): File {
            val wrapper = gradleWrappers.getOrPut(version) { createNewWrapperDir(version) }

            if (withDaemon) {
                DaemonRegistry.register(version)

                if (DaemonRegistry.activeDaemons.size > MAX_ACTIVE_GRADLE_PROCESSES) {
                    println("Too many Gradle active processes (max is $MAX_ACTIVE_GRADLE_PROCESSES). Stopping all daemons")
                    stopAllDaemons(environmentVariables)
                }

                if (DaemonRegistry.runCountForDaemon(version) >= MAX_DAEMON_RUNS) {
                    // Warning: Stopping the Gradle daemon while the test suite has not finished running can break tests in weird ways.
                    // TODO: Find a safe way to do this or consider deleting this code.
                    stopDaemon(version, environmentVariables)
                }

                // we could've stopped daemon
                if (DaemonRegistry.runCountForDaemon(version) <= 0) {
                    DaemonRegistry.register(version)
                }
            }

            return wrapper
        }

        fun maybeUpdateSettingsScript(wrapperVersion: String, settingsScript: File) {
            // enableFeaturePreview("GRADLE_METADATA") is no longer needed when building with Gradle 5.4 or above
            if (GradleVersion.version(wrapperVersion) >= GradleVersion.version("5.4")) {
                settingsScript.apply {
                    if (exists()) {
                        modify {
                            it.replace("enableFeaturePreview('GRADLE_METADATA')", "//")
                        }
                        modify {
                            it.replace("enableFeaturePreview(\"GRADLE_METADATA\")", "//")
                        }
                    }
                }
            }
        }

        private fun createNewWrapperDir(version: String): File =
            createTempDir("GradleWrapper-$version-")
                .apply {
                    File(BaseGradleIT.resourcesRootFile, "GradleWrapper").copyRecursively(this)
                    val wrapperProperties = File(this, "gradle/wrapper/gradle-wrapper.properties")
                    val isGradleVerisonSnapshot = version.endsWith("+0000")
                    if (!isGradleVerisonSnapshot) {
                        wrapperProperties.modify { it.replace("<GRADLE_WRAPPER_VERSION>", version) }
                    } else {
                        wrapperProperties.modify {
                            it.replace("distributions/gradle-<GRADLE_WRAPPER_VERSION>", "distributions-snapshots/gradle-$version")
                        }
                    }
                }

        private val runnerGradleVersion = System.getProperty("runnerGradleVersion")

        private fun stopDaemon(version: String, environmentVariables: Map<String, String>) {
            assert(version != runnerGradleVersion) { "Not stopping Gradle daemon v$version as it matches the runner version" }
            println("Stopping gradle daemon v$version")

            val wrapperDir = gradleWrappers[version] ?: error("Was asked to stop unknown daemon $version")
            val cmd = createGradleCommand(wrapperDir, arrayListOf("-stop"))
            val result = runProcess(cmd, wrapperDir, environmentVariables)
            assert(result.isSuccessful) { "Could not stop daemon: $result" }
            DaemonRegistry.unregister(version)
        }

        private fun stopAllDaemons(environmentVariables: Map<String, String>) {
            for (version in DaemonRegistry.activeDaemons) {
                stopDaemon(version, environmentVariables)
            }
            assert(DaemonRegistry.activeDaemons.isEmpty()) {
                "Could not stop some daemons ${(DaemonRegistry.activeDaemons).joinToString()}"
            }
        }
    }

    // the second parameter is for using with ToolingAPI, that do not like --daemon/--no-daemon  options at all
    data class BuildOptions constructor(
        val withDaemon: Boolean = false,
        val daemonOptionSupported: Boolean = true,
        val incremental: Boolean? = null,
        val incrementalJs: Boolean? = null,
        val incrementalJsKlib: Boolean? = null,
        val jsIrBackend: Boolean? = null,
        val androidHome: File? = null,
        val javaHome: File? = null,
        val gradleUserHome: File? = null,
        val androidGradlePluginVersion: AGPVersion? = null,
        val forceOutputToStdout: Boolean = false,
        val debug: Boolean = false,
        val freeCommandLineArgs: List<String> = emptyList(),
        val kotlinVersion: String = KOTLIN_VERSION,
        val kotlinDaemonDebugPort: Int? = null,
        val usePreciseJavaTracking: Boolean? = null,
        val useClasspathSnapshot: Boolean? = null,
        val withBuildCache: Boolean = false,
        val kaptOptions: KaptOptions? = null,
        val parallelTasksInProject: Boolean = false,
        val jsCompilerType: KotlinJsCompilerType? = null,
        val configurationCache: Boolean = false,
        val configurationCacheProblems: ConfigurationCacheProblems = ConfigurationCacheProblems.FAIL,
        val warningMode: WarningMode = WarningMode.Fail,
        val useFir: Boolean = false,
        val customEnvironmentVariables: Map<String, String> = mapOf(),
        val dryRun: Boolean = false,
        val abiSnapshot: Boolean = false,
        val hierarchicalMPPStructureSupport: Boolean? = null,
        val enableCompatibilityMetadataVariant: Boolean? = null,
        val withReports: List<BuildReportType> = emptyList(),
        val enableKpmModelMapping: Boolean? = null,
    )

    enum class ConfigurationCacheProblems {
        FAIL, WARN
    }

    data class KaptOptions(
        val verbose: Boolean,
        val useWorkers: Boolean,
        val incrementalKapt: Boolean = false,
        val includeCompileClasspath: Boolean = true,
        val classLoadersCacheSize: Int? = null
    )

    open inner class Project(
        val projectName: String,
        val gradleVersionRequirement: GradleVersionRequired = defaultGradleVersion,
        directoryPrefix: String? = null,
        val minLogLevel: LogLevel = LogLevel.DEBUG,
        val addHeapDumpOptions: Boolean = true
    ) {
        internal val testCase = this@BaseGradleIT

        val resourceDirName = if (directoryPrefix != null) "$directoryPrefix/$projectName" else projectName
        open val resourcesRoot = File(resourcesRootFile, "testProject/$resourceDirName")
        val projectDir = File(workingDir.canonicalFile, projectName)

        open fun setupWorkingDir(enableCacheRedirector: Boolean = true, applyAndroidTestFixes: Boolean = true) {
            if (!projectDir.isDirectory || projectDir.listFiles().isEmpty()) {
                copyRecursively(this.resourcesRoot, workingDir)
                if (addHeapDumpOptions) {
                    addHeapDumpOptionsToPropertiesFile()
                }

                projectDir.toPath().apply {
                    addPluginManagementToSettings()
                    if (enableCacheRedirector) enableCacheRedirector()
                    if (applyAndroidTestFixes) applyAndroidTestFixes()
                }
            }
        }

        private fun addHeapDumpOptionsToPropertiesFile() {
            val propertiesFile = File(projectDir, "gradle.properties")
            propertiesFile.createNewFile()

            val heapDumpOutOfErrorStr = "-XX:+HeapDumpOnOutOfMemoryError"
            val heapDumpPathStr = "-XX:HeapDumpPath=\"${System.getProperty("user.dir")}${File.separatorChar}build\""

            val gradlePropertiesText = propertiesFile.readText()

            val presentJvmArgsLine = gradlePropertiesText
                .lines()
                .singleOrNull { it.contains("org.gradle.jvmargs") } // Can't write back if there are several lines with jvmargs

            val updated: Boolean
            val updatedJvmArgsLine = if (presentJvmArgsLine == null) {
                updated = true
                "org.gradle.jvmargs=$heapDumpOutOfErrorStr $heapDumpPathStr"
            } else {
                val options = buildString {
                    if (!presentJvmArgsLine.contains("HeapDumpOnOutOfMemoryError")) {
                        append(" $heapDumpOutOfErrorStr")
                    }
                    if (!presentJvmArgsLine.contains("HeapDumpPath")) {
                        append(" $heapDumpPathStr")
                    }
                }

                if (options.isEmpty()) {
                    // All options are already present
                    updated = false
                    presentJvmArgsLine
                } else {
                    updated = true
                    "$presentJvmArgsLine$options"
                }
            }

            if (!updated) {
                return
            }

            val lines = listOf("# modified in addHeapDumpOptionsToPropertiesFile", updatedJvmArgsLine) +
                    gradlePropertiesText.lines().filter { !it.contains("org.gradle.jvmargs") }

            propertiesFile.writeText(lines.joinToString(separator = "\n"))
        }

        fun relativize(files: Iterable<File>): List<String> =
            files.map { it.relativeTo(projectDir).path }

        fun relativize(vararg files: File): List<String> =
            files.map { it.relativeTo(projectDir).path }
    }

    class CompiledProject(val project: Project, val output: String, val resultCode: Int)

    // Basically the same as `Project.build`, tells gradle to wait for debug on 5005 port
    // Faster to type than `project.build("-Dorg.gradle.debug=true")` or `project.build(options = defaultBuildOptions().copy(debug = true))`
    @Deprecated("Use, but do not commit!")
    fun Project.debug(vararg params: String, options: BuildOptions = defaultBuildOptions(), check: CompiledProject.() -> Unit) {
        build(*params, options = options.copy(debug = true), check = check)
    }

    @Deprecated("Use, but do not commit!")
    fun Project.debugKotlinDaemon(
        vararg params: String,
        debugPort: Int = 5006,
        options: BuildOptions = defaultBuildOptions(),
        check: CompiledProject.() -> Unit
    ) {
        build(*params, options = options.copy(kotlinDaemonDebugPort = debugPort), check = check)
    }

    fun Project.build(
        vararg params: String,
        options: BuildOptions = defaultBuildOptions(),
        projectDir: File = File(workingDir, projectName),
        check: CompiledProject.() -> Unit
    ) {
        val wrapperVersion = chooseWrapperVersionOrFinishTest()

        // TODO: remove this when the minimal Gradle version is >= 6.9
        val buildOptions = if (HostManager.host == KonanTarget.MACOS_ARM64) {
            val minSupportedMacAArch64Version = GradleVersion.version("6.9")
            val withDaemon = (GradleVersion.version(wrapperVersion) >= minSupportedMacAArch64Version)
            options.copy(withDaemon = withDaemon && options.withDaemon)
        } else options

        val env = createEnvironmentVariablesMap(buildOptions)
        val wrapperDir = prepareWrapper(wrapperVersion, env)

        val cmd = createBuildCommand(wrapperDir, params, buildOptions)

        if (!projectDir.exists()) {
            setupWorkingDir()
        }

        maybeUpdateSettingsScript(wrapperVersion, gradleSettingsScript())

        var result: ProcessRunResult? = null
        try {
            result = runProcess(cmd, projectDir, env, buildOptions)
            CompiledProject(this, result.output, result.exitCode).check()
        } catch (t: Throwable) {
            println("<=== Test build: ${this.projectName} $cmd ===>")

            // to prevent duplication of output
            if (!options.forceOutputToStdout && result != null) {
                println(result.output)
            }
            throw t
        }
    }

    fun <T> Project.getModels(modelType: Class<T>): ModelContainer<T> {
        if (!projectDir.exists()) {
            setupWorkingDir()
        }

        val options = defaultBuildOptions()
        val arguments = mutableListOf("-Pkotlin_version=${options.kotlinVersion}", "-Ptest_fixes_version=$KOTLIN_VERSION")
        options.androidGradlePluginVersion?.let { arguments.add("-Pandroid_tools_version=$it") }
        val env = createEnvironmentVariablesMap(options)
        val wrapperVersion = chooseWrapperVersionOrFinishTest()
        prepareWrapper(wrapperVersion, env)

        val connection = GradleConnector
            .newConnector()
            .useGradleVersion(wrapperVersion)
            .forProjectDirectory(projectDir)
            .connect()
        val model = connection.action(ModelFetcherBuildAction(modelType)).withArguments(arguments).setEnvironmentVariables(env).run()
        connection.close()
        return model
    }

    fun CompiledProject.assertSuccessful(message: String? = null) {
        if (resultCode == 0) return

        val errors = "(?m)^.*\\[ERROR] \\[\\S+] (.*)$".toRegex().findAll(output)
        val errorMessage = buildString {
            if (message != null) appendLine(message)
            appendLine("Gradle build failed")
            appendLine()
            if (errors.any()) {
                appendLine("Possible errors:")
                errors.forEach { match -> appendLine(match.groupValues[1]) }
            }
        }
        fail(errorMessage)
    }

    fun CompiledProject.assertFailed(): CompiledProject {
        assertNotEquals(0, resultCode, "Expected that Gradle build failed")
        return this
    }

    fun CompiledProject.assertContains(vararg expected: String, ignoreCase: Boolean = false): CompiledProject {
        for (str in expected) {
            assertTrue(output.contains(str.normalize(), ignoreCase), "Output should contain '$str'")
        }
        return this
    }

    fun CompiledProject.assertContainsRegex(expected: Regex): CompiledProject {
        assertTrue(expected.containsMatchIn(output), "Output should contain pattern '$expected'")
        return this
    }

    fun CompiledProject.assertClassFilesNotContain(dir: File, vararg strings: String) {
        val classFiles = dir.walk().filter { it.isFile && it.extension.toLowerCase() == "class" }

        for (cf in classFiles) {
            checkBytecodeNotContains(cf, strings.toList())
        }
    }

    fun CompiledProject.assertSubstringCount(substring: String, expectedCount: Int) {
        val actualCount = Pattern.quote(substring).toRegex().findAll(output).count()
        assertEquals(expectedCount, actualCount, "Number of occurrences in output for substring '$substring'")
    }

    fun CompiledProject.checkKotlinGradleBuildServices() {
        assertSubstringCount("Initialized KotlinGradleBuildServices", expectedCount = 1)
        assertSubstringCount("Disposed KotlinGradleBuildServices", expectedCount = 1)
    }

    fun CompiledProject.assertNotContains(vararg expected: String): CompiledProject {
        for (str in expected) {
            assertFalse(output.contains(str.normalize()), "Output should not contain '$str'")
        }
        return this
    }

    fun CompiledProject.assertNotContains(regex: Regex) {
        assertNull(regex.find(output)?.value, "Output should not contain '$regex'")
    }

    fun CompiledProject.assertNoWarnings(sanitize: (String) -> String = { it }) {
        val clearedOutput = sanitize(output)
        val warnings = "w: .*".toRegex().findAll(clearedOutput).map { it.groupValues[0] }

        if (warnings.any()) {
            val message = (listOf("Output should not contain any warnings:") + warnings).joinToString(SYSTEM_LINE_SEPARATOR)
            throw IllegalStateException(message)
        }
    }

    fun CompiledProject.fileInWorkingDir(path: String) = File(File(workingDir, project.projectName), path)

    fun CompiledProject.assertReportExists(pathToReport: String = ""): CompiledProject {
        assertTrue(fileInWorkingDir(pathToReport).exists(), "The report [$pathToReport] does not exist.")
        return this
    }

    fun CompiledProject.assertSingleFileExists(
        directory: String = "",
        filePath: String = ""
    ): CompiledProject {
        val directoryFile = fileInWorkingDir(directory)
        assertTrue(
            directoryFile.listFiles()?.size == 1,
            "[$directory] should contain only single file"
        )
        return assertFileExists("$directory/$filePath")
    }

    fun CompiledProject.assertFileExists(path: String = ""): CompiledProject {
        assertTrue(fileInWorkingDir(path).exists(), "The file [$path] does not exist.")
        return this
    }

    fun CompiledProject.assertFileIsSymlink(path: String = ""): CompiledProject {
        assertTrue(Files.isSymbolicLink(fileInWorkingDir(path).toPath()), "The file [$path] isn't a symlink.")
        return this
    }

    fun CompiledProject.assertNoSuchFile(path: String = ""): CompiledProject {
        assertFalse(fileInWorkingDir(path).exists(), "The file [$path] exists.")
        return this
    }

    fun CompiledProject.assertFileContains(path: String, vararg expected: String): CompiledProject {
        val text = fileInWorkingDir(path).readText()
        expected.forEach {
            assertTrue(text.contains(it), "$path should contain '$it', actual file contents:\n$text")
        }
        return this
    }

    internal fun Iterable<File>.projectRelativePaths(project: Project): Iterable<String> {
        return map { it.canonicalFile.toRelativeString(project.projectDir) }
    }

    fun CompiledProject.findTasksByPattern(pattern: String): Set<String> {
        return "task '($pattern)'".toRegex().findAll(output).mapTo(HashSet()) { it.groupValues[1] }
    }

    fun CompiledProject.assertTasksExecuted(tasks: Iterable<String>) {
        for (task in tasks) {
            assertContainsRegex("(Executing actions for task|Executing task) '$task'".toRegex())
        }
    }

    fun CompiledProject.assertTasksNotExecuted(tasks: Iterable<String>) {
        for (task in tasks) {
            assertNotContains("(Executing actions for task|Executing task) '$task'".toRegex())
        }
    }

    fun CompiledProject.assertTasksExecutedByPrefix(taskPrefixes: Iterable<String>) {
        for (prefix in taskPrefixes) {
            assertContainsRegex("(Executing actions for task|Executing task) '$prefix\\w*'".toRegex())
        }
    }

    fun CompiledProject.assertTasksExecuted(vararg tasks: String) {
        assertTasksExecuted(tasks.toList())
    }

    fun CompiledProject.assertTasksNotExecuted(vararg tasks: String) {
        assertTasksNotExecuted(tasks.toList())
    }

    fun CompiledProject.assertTasksRetrievedFromCache(tasks: Iterable<String>) {
        for (task in tasks) {
            assertContains("$task FROM-CACHE")
        }
    }

    fun CompiledProject.assertTasksRetrievedFromCache(vararg tasks: String) {
        assertTasksRetrievedFromCache(tasks.toList())
    }

    fun CompiledProject.assertTasksUpToDate(tasks: Iterable<String>) {
        for (task in tasks) {
            assertContains("$task UP-TO-DATE")
        }
    }

    fun CompiledProject.assertTasksFailed(vararg tasks: String) {
        assertTasksFailed(tasks.toList())
    }

    fun CompiledProject.assertTasksFailed(tasks: Iterable<String>) {
        for (task in tasks) {
            assertContains("$task FAILED")
        }
    }

    fun CompiledProject.assertTasksUpToDate(vararg tasks: String) {
        assertTasksUpToDate(tasks.toList())
    }

    fun CompiledProject.assertTasksSubmittedWork(vararg tasks: String) {
        for (task in tasks) {
            assertContains("Starting Kotlin compiler work from task '$task'")
        }
    }

    fun CompiledProject.assertTasksDidNotSubmitWork(vararg tasks: String) {
        for (task in tasks) {
            assertNotContains("Starting Kotlin compiler work from task '$task'")
        }
    }

    fun CompiledProject.assertTasksRegistered(vararg tasks: String) {
        for (task in tasks) {
            assertContains("'Register task $task'")
        }
    }

    fun CompiledProject.assertTasksRegisteredRegex(vararg tasks: String) {
        for (task in tasks) {
            assertContainsRegex("'Register task $task'".toRegex())
        }
    }

    fun CompiledProject.assertTasksNotRegistered(vararg tasks: String) {
        for (task in tasks) {
            assertNotContains("'Register task $task'")
        }
    }

    fun CompiledProject.assertTasksRegisteredByPrefix(taskPrefixes: Iterable<String>) {
        for (prefix in taskPrefixes) {
            assertContainsRegex("'Register task $prefix\\w*'".toRegex())
        }
    }

    fun CompiledProject.assertTasksNotRegisteredByPrefix(taskPrefixes: Iterable<String>) {
        for (prefix in taskPrefixes) {
            assertNotContains("'Register task $prefix\\w*'".toRegex())
        }
    }

    fun CompiledProject.assertTasksNotRealized(vararg tasks: String) {
        for (task in tasks) {
            assertNotContains("'Realize task $task'")
        }
    }

    fun CompiledProject.assertTasksNotRealizedRegex(vararg tasks: String) {
        for (task in tasks) {
            assertNotContains("'Realize task $task'".toRegex())
        }
    }

    fun CompiledProject.assertTasksRegisteredAndNotRealized(vararg tasks: String) {
        assertTasksRegistered(*tasks)
        assertTasksNotRealized(*tasks)
    }

    fun CompiledProject.assertTasksSkipped(vararg tasks: String) {
        for (task in tasks) {
            assertContains("Skipping task '$task'")
        }
    }

    fun CompiledProject.assertTasksSkippedByPrefix(taskPrefixes: Iterable<String>) {
        for (prefix in taskPrefixes) {
            assertContainsRegex("Skipping task '$prefix\\w*'".toRegex())
        }
    }

    fun CompiledProject.getOutputForTask(taskName: String): String {
        @Language("RegExp")
        val taskOutputRegex = """
(?:
\[LIFECYCLE] \[class org\.gradle(?:\.internal\.buildevents)?\.TaskExecutionLogger] :$taskName|
\[org\.gradle\.execution\.(?:plan|taskgraph)\.Default(?:Task)?PlanExecutor] :$taskName.*?started
)
([\s\S]+?)
(?:
Finished executing task ':$taskName'|
\[org\.gradle\.execution\.(?:plan|taskgraph)\.Default(?:Task)?PlanExecutor] :$taskName.*?completed
)
""".trimIndent().replace("\n", "").toRegex()

        return taskOutputRegex.find(output)?.run { groupValues[1] } ?: error("Cannot find output for task $taskName")
    }

    fun CompiledProject.assertCompiledKotlinSources(
        sources: Iterable<String>,
        weakTesting: Boolean = false,
        tasks: List<String>
    ) {
        for (task in tasks) {
            assertCompiledKotlinSources(sources, weakTesting, getOutputForTask(task), suffix = " in task ${task}")
        }
    }

    fun CompiledProject.assertCompiledKotlinSources(
        expectedSourcesRelativePaths: Iterable<String>,
        weakTesting: Boolean = false,
        output: String = this.output,
        suffix: String = ""
    ): CompiledProject {
        val messagePrefix = "Compiled Kotlin files differ${suffix}:\n"
        val actualSources = extractCompiledKotlinFiles(output)
        if (weakTesting) {
            assertContainsFiles(expectedSourcesRelativePaths.toPaths(), actualSources, messagePrefix)
        } else {
            assertSameFiles(expectedSourcesRelativePaths.toPaths(), actualSources, messagePrefix)
        }
        return this
    }

    fun CompiledProject.assertCompiledKotlinFiles(expectedFiles: Iterable<File>): CompiledProject =
        assertCompiledKotlinSources(project.relativize(expectedFiles))

    val Project.allKotlinFiles: Iterable<File>
        get() = projectDir.allKotlinFiles()

    fun Project.projectFile(name: String): File =
        projectDir.getFileByName(name)

    fun CompiledProject.assertCompiledJavaSources(
        sources: Iterable<String>,
        weakTesting: Boolean = false
    ): CompiledProject {
        val messagePrefix = "Compiled Java files differ:\n"
        val actualSources = extractCompiledJavaFiles(project.projectDir, output)
        if (weakTesting)
            assertContainsFiles(sources.toPaths(), actualSources, messagePrefix)
        else
            assertSameFiles(sources.toPaths(), actualSources, messagePrefix)
        return this
    }

    fun Project.resourcesDir(subproject: String? = null, sourceSet: String = "main"): String =
        (subproject?.plus("/") ?: "") + "build/resources/$sourceSet/"

    fun Project.classesDir(subproject: String? = null, sourceSet: String = "main", language: String = "kotlin"): String =
        (subproject?.plus("/") ?: "") + "build/classes/$language/$sourceSet/"

    fun Project.testGradleVersionAtLeast(version: String): Boolean =
        GradleVersion.version(chooseWrapperVersionOrFinishTest()) >= GradleVersion.version(version)

    fun Project.testGradleVersionBelow(version: String): Boolean = !testGradleVersionAtLeast(version)

    fun CompiledProject.kotlinClassesDir(subproject: String? = null, sourceSet: String = "main"): String =
        project.classesDir(subproject, sourceSet, language = "kotlin")

    fun CompiledProject.javaClassesDir(subproject: String? = null, sourceSet: String = "main"): String =
        project.classesDir(subproject, sourceSet, language = "java")

    fun CompiledProject.compilerArgs(taskName: String): String {
        val pattern = "$taskName Kotlin compiler args: "
        return output
            .lineSequence()
            .firstOrNull { it.contains(pattern) }
            ?.substringAfter(pattern)
            ?: throw AssertionError("Cant find compiler args for task: $taskName")
    }

    private fun Project.createBuildCommand(wrapperDir: File, params: Array<out String>, options: BuildOptions): List<String> =
        createGradleCommand(wrapperDir, createGradleTailParameters(options, params))

    fun Project.gradleBuildScript(subproject: String? = null): File =
        listOf("build.gradle", "build.gradle.kts").mapNotNull {
            File(projectDir, subproject?.plus("/").orEmpty() + it).takeIf(File::exists)
        }.single()

    fun Project.gradleSettingsScript(): File =
        listOf("settings.gradle", "settings.gradle.kts").map {
            File(projectDir, it)
        }.run {
            singleOrNull { it.exists() } ?: first()
        }

    fun Project.gradleProperties(): File =
        File(projectDir, "gradle.properties").also { file ->
            if (!file.exists()) {
                file.createNewFile()
            }
        }

    /**
     * @param assertionFileName path to xml with expected test results, relative to test resources root
     */
    fun CompiledProject.assertTestResults(
        @TestDataFile assertionFileName: String,
        vararg testReportNames: String
    ) {
        val projectDir = project.projectDir
        val testReportDirs = testReportNames.map { projectDir.resolve("build/test-results/$it").toPath() }

        testReportDirs.forEach {
            if (!it.isDirectory()) {
                error("Test report dir $it was not created")
            }
        }

        val actualTestResults = readAndCleanupTestResults(testReportDirs, projectDir.toPath()) { s ->
            val excl = "Invalid connection: com.apple.coresymbolicationd"
            s.lines().filter { it != excl }.joinToString("\n")
        }
        val expectedTestResults = prettyPrintXml(resourcesRootFile.resolve(assertionFileName).readText())

        assertEquals(expectedTestResults, actualTestResults)
    }

    private fun Project.createGradleTailParameters(options: BuildOptions, params: Array<out String> = arrayOf()): List<String> =
        params.toMutableList().apply {
            add("--stacktrace")
            when (minLogLevel) {
                // Do not allow to configure Gradle project with `ERROR` log level (error logs visible on all log levels)
                LogLevel.ERROR -> error("Log level ERROR is not supported by Gradle command-line")
                // Omit log level argument for default `LIFECYCLE` log level,
                // because there is no such command-line option `--lifecycle`
                // see https://docs.gradle.org/current/userguide/logging.html#sec:choosing_a_log_level
                LogLevel.LIFECYCLE -> Unit
                //Command line option for other log levels
                else -> add("--${minLogLevel.name.toLowerCase()}")
            }
            if (options.daemonOptionSupported) {
                add(if (options.withDaemon) "--daemon" else "--no-daemon")
            }

            add("-Pkotlin_version=" + options.kotlinVersion)
            add("-Ptest_fixes_version=$KOTLIN_VERSION")
            options.incremental?.let {
                add("-Pkotlin.incremental=$it")
            }
            options.incrementalJs?.let { add("-Pkotlin.incremental.js=$it") }
            options.incrementalJsKlib?.let { add("-Pkotlin.incremental.js.klib=$it") }
            options.jsIrBackend?.let { add("-Pkotlin.js.useIrBackend=$it") }
            options.usePreciseJavaTracking?.let { add("-Pkotlin.incremental.usePreciseJavaTracking=$it") }
            options.useClasspathSnapshot?.let { add("-P${COMPILE_INCREMENTAL_WITH_ARTIFACT_TRANSFORM.property}=$it") }
            options.androidGradlePluginVersion?.let { add("-Pandroid_tools_version=$it") }
            if (options.debug) {
                add("-Dorg.gradle.debug=true")
            }
            options.kotlinDaemonDebugPort?.let { port ->
                add("-Dkotlin.daemon.jvm.options=-agentlib:jdwp=transport=dt_socket\\,server=y\\,suspend=y\\,address=$port")
            }
            System.getProperty("maven.repo.local")?.let {
                add("-Dmaven.repo.local=$it") // TODO: proper escaping
            }

            if (options.withBuildCache) {
                add("--build-cache")
            } else {
                // Override possibly enabled system-wide caching:
                add("-Dorg.gradle.caching=false")
            }

            options.kaptOptions?.also { kaptOptions ->
                add("-Pkapt.verbose=${kaptOptions.verbose}")
                add("-Pkapt.use.worker.api=${kaptOptions.useWorkers}")
                add("-Pkapt.incremental.apt=${kaptOptions.incrementalKapt}")
                add("-Pkapt.include.compile.classpath=${kaptOptions.includeCompileClasspath}")
                kaptOptions.classLoadersCacheSize?.also { cacheSize ->
                    add("-Pkapt.classloaders.cache.size=$cacheSize")
                }
            }

            if (options.parallelTasksInProject) add("--parallel") else add("--no-parallel")

            options.jsCompilerType?.let {
                add("-Pkotlin.js.compiler=$it")
            }

            if (options.useFir) {
                add("-Pkotlin.useFir=true")
            }

            if (options.dryRun) {
                add("--dry-run")
            }
            if (options.abiSnapshot) {
                add("-Pkotlin.incremental.classpath.snapshot.enabled=true")
            }

            if (options.hierarchicalMPPStructureSupport != null) {
                add("-Pkotlin.mpp.hierarchicalStructureSupport=${options.hierarchicalMPPStructureSupport}")
            }

            if (options.enableCompatibilityMetadataVariant != null) {
                add("-Pkotlin.mpp.enableCompatibilityMetadataVariant=${options.enableCompatibilityMetadataVariant}")
            }

            if (options.withReports.isNotEmpty()) {
                add("-Pkotlin.build.report.output=${options.withReports.joinToString { it.name }}")
            }

            if (options.enableKpmModelMapping != null) {
                add("-Pkotlin.kpm.experimentalModelMapping=${options.enableKpmModelMapping}")
            }

            add("-Dorg.gradle.unsafe.configuration-cache=${options.configurationCache}")
            add("-Dorg.gradle.unsafe.configuration-cache-problems=${options.configurationCacheProblems.name.toLowerCase()}")

            // Workaround: override a console type set in the user machine gradle.properties (since Gradle 4.3):
            add("--console=plain")
            //The feature of failing the build on deprecation warnings is introduced in gradle 5.6
            val supportFailingBuildOnWarning =
                GradleVersion.version(chooseWrapperVersionOrFinishTest()) >= GradleVersion.version("5.6")
            // Agp uses Gradle internal API constructor DefaultDomainObjectSet(Class<T>) until Agp 3.6.0 which is deprecated by Gradle,
            // so we don't run with --warning-mode=fail when Agp 3.6 or less is used.
            val notUsingAgpWithWarnings =
                options.androidGradlePluginVersion == null || options.androidGradlePluginVersion > AGPVersion.v3_6_0
            if (supportFailingBuildOnWarning && notUsingAgpWithWarnings && options.warningMode == WarningMode.Fail) {
                add("--warning-mode=${WarningMode.Fail.name.toLowerCase()}")
            }
            addAll(options.freeCommandLineArgs)
        }

    private fun createEnvironmentVariablesMap(options: BuildOptions): Map<String, String> =
        hashMapOf<String, String>().apply {
            options.androidHome?.let { sdkDir ->
                sdkDir.parentFile.mkdirs()
                put("ANDROID_HOME", sdkDir.canonicalPath)
            }

            options.javaHome?.let {
                put("JAVA_HOME", it.canonicalPath)
            }

            options.gradleUserHome?.let {
                put("GRADLE_USER_HOME", it.canonicalPath)
            }
            putAll(options.customEnvironmentVariables)
        }

    private fun String.normalize() = this.lineSequence().joinToString(SYSTEM_LINE_SEPARATOR)

    fun copyRecursively(source: File, target: File) {
        assertTrue(target.isDirectory)
        val targetFile = File(target, source.name)
        if (source.isDirectory) {
            targetFile.mkdir()
            source.listFiles()?.forEach { copyRecursively(it, targetFile) }
        } else {
            source.copyTo(targetFile)
        }
    }

    fun copyDirRecursively(source: File, target: File) {
        assertTrue(source.isDirectory)
        assertTrue(target.isDirectory)
        source.listFiles()?.forEach { copyRecursively(it, target) }
    }

    private fun String.normalizePath() = replace("\\", "/")
}

fun BaseGradleIT.BuildOptions.withFreeCommandLineArgument(argument: String) = copy(
    freeCommandLineArgs = freeCommandLineArgs + argument
)

private const val MAVEN_LOCAL_URL_PLACEHOLDER = "<mavenLocalUrl>"
internal const val PLUGIN_MARKER_VERSION_PLACEHOLDER = "<pluginMarkerVersion>"

internal fun BaseGradleIT.transformProjectWithPluginsDsl(
    projectName: String,
    wrapperVersion: GradleVersionRequired = defaultGradleVersion,
    directoryPrefix: String? = null,
    minLogLevel: LogLevel = LogLevel.DEBUG
): BaseGradleIT.Project {

    val result = Project(projectName, wrapperVersion, directoryPrefix, minLogLevel)
    result.setupWorkingDir()

    val settingsGradle = File(result.projectDir, "settings.gradle").takeIf(File::exists)
    settingsGradle?.modify {
        it.replace(MAVEN_LOCAL_URL_PLACEHOLDER, MavenLocalUrlProvider.mavenLocalUrl)
    }

    result.projectDir.walkTopDown()
        .filter {
            it.isFile && (it.name == "build.gradle" || it.name == "build.gradle.kts" ||
                    it.name == "settings.gradle" || it.name == "settings.gradle.kts")
        }
        .forEach { buildGradle ->
            buildGradle.modify(::transformBuildScriptWithPluginsDsl)
        }

    return result
}

internal fun transformBuildScriptWithPluginsDsl(buildScriptContent: String): String =
    buildScriptContent.replace(PLUGIN_MARKER_VERSION_PLACEHOLDER, KOTLIN_VERSION)

/** Copies the logic of Gradle [`mavenLocal()`](https://docs.gradle.org/3.4.1/dsl/org.gradle.api.artifacts.dsl.RepositoryHandler.html#org.gradle.api.artifacts.dsl.RepositoryHandler:mavenLocal())
 */
private object MavenLocalUrlProvider {
    /** The URL that points to the Gradle's mavenLocal() repository. */
    val mavenLocalUrl by lazy {
        val path = propertyMavenLocalRepoPath ?: homeSettingsLocalRepoPath ?: m2HomeSettingsLocalRepoPath ?: defaultM2RepoPath
        File(path).toURI().toString()
    }

    private val homeDir get() = File(System.getProperty("user.home"))

    private fun getLocalRepositoryFromXml(file: File): String? {
        if (!file.isFile)
            return null

        val xml = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val localRepoNodes = xml.getElementsByTagName("localRepository")

        if (localRepoNodes.length == 0)
            return null

        val content = localRepoNodes.item(0).textContent

        return content.replace("\\$\\{(.*?)\\}".toRegex()) { System.getProperty(it.groupValues[1]) ?: it.value }
    }

    private val propertyMavenLocalRepoPath get() = System.getProperty("maven.repo.local")

    private val homeSettingsLocalRepoPath
        get() = getLocalRepositoryFromXml(File(homeDir, ".m2/settings.xml"))

    private val m2HomeSettingsLocalRepoPath
        get() = System.getProperty("M2_HOME")?.let { getLocalRepositoryFromXml(File(it, "conf/settings.xml")) }

    private val defaultM2RepoPath get() = File(homeDir, ".m2/repository").absolutePath
}
