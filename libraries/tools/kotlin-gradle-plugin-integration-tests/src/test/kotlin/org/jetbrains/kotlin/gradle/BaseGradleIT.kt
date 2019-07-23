package org.jetbrains.kotlin.gradle

import com.intellij.testFramework.TestDataFile
import org.gradle.api.logging.LogLevel
import org.gradle.tooling.GradleConnector
import org.gradle.util.GradleVersion
import org.jdom.Element
import org.jdom.input.SAXBuilder
import org.jdom.output.Format
import org.jdom.output.XMLOutputter
import org.jetbrains.kotlin.gradle.model.ModelContainer
import org.jetbrains.kotlin.gradle.model.ModelFetcherBuildAction
import org.jetbrains.kotlin.gradle.util.*
import org.jetbrains.kotlin.test.util.trimTrailingWhitespaces
import org.junit.After
import org.junit.AfterClass
import org.junit.Assert
import org.junit.Before
import java.io.File
import java.util.regex.Pattern
import kotlin.test.*

val SYSTEM_LINE_SEPARATOR: String = System.getProperty("line.separator")

abstract class BaseGradleIT {

    protected var workingDir = File(".")

    protected open fun defaultBuildOptions(): BuildOptions = BuildOptions(withDaemon = true)

    open val defaultGradleVersion: GradleVersionRequired
        get() = GradleVersionRequired.None

    @Before
    fun setUp() {
        workingDir = createTempDir("BaseGradleIT")
        acceptAndroidSdkLicenses()
    }

    @After
    fun tearDown() {
        workingDir.deleteRecursively()
    }

    // https://developer.android.com/studio/intro/update.html#download-with-gradle
    fun acceptAndroidSdkLicenses() = defaultBuildOptions().androidHome?.let {
        val sdkLicenses = File(it, "licenses")
        sdkLicenses.mkdirs()

        val sdkLicense = File(sdkLicenses, "android-sdk-license")
        if (!sdkLicense.exists()) {
            sdkLicense.createNewFile()
            sdkLicense.writeText(
                """
                8933bad161af4178b1185d1a37fbf41ea5269c55
                d56f5187479451eabf01fb78af6dfcb131a6481e
                24333f8a63b6825ea9c5514f83c2829b004d1fee
                """.trimIndent()
            )
        }

        val sdkPreviewLicense = File(sdkLicenses, "android-sdk-preview-license")
        if (!sdkPreviewLicense.exists()) {
            sdkPreviewLicense.writeText("84831b9409646a918e30573bab4c9c91346d8abd")
        }
    }

    companion object {

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
        private const val MAX_DAEMON_RUNS = 30
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
                    stopDaemon(version, environmentVariables)
                }

                // we could've stopped daemon
                if (DaemonRegistry.runCountForDaemon(version) <= 0) {
                    DaemonRegistry.register(version)
                }
            }

            return wrapper
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
        val androidHome: File? = null,
        val javaHome: File? = null,
        val androidGradlePluginVersion: AGPVersion? = null,
        val forceOutputToStdout: Boolean = false,
        val debug: Boolean = false,
        val freeCommandLineArgs: List<String> = emptyList(),
        val kotlinVersion: String = KOTLIN_VERSION,
        val kotlinDaemonDebugPort: Int? = null,
        val usePreciseJavaTracking: Boolean? = null,
        val withBuildCache: Boolean = false,
        val kaptOptions: KaptOptions? = null,
        val parallelTasksInProject: Boolean? = null
    )

    data class KaptOptions(val verbose: Boolean, val useWorkers: Boolean, val incrementalKapt: Boolean = false, val includeCompileClasspath: Boolean = true)

    open inner class Project(
        val projectName: String,
        val gradleVersionRequirement: GradleVersionRequired = defaultGradleVersion,
        directoryPrefix: String? = null,
        val minLogLevel: LogLevel = LogLevel.DEBUG
    ) {
        internal val testCase = this@BaseGradleIT

        val resourceDirName = if (directoryPrefix != null) "$directoryPrefix/$projectName" else projectName
        open val resourcesRoot = File(resourcesRootFile, "testProject/$resourceDirName")
        val projectDir = File(workingDir.canonicalFile, projectName)

        open fun setupWorkingDir() {
            if (!projectDir.isDirectory || projectDir.listFiles().isEmpty())
                copyRecursively(this.resourcesRoot, workingDir)
        }

        fun relativize(files: Iterable<File>): List<String> =
            files.map { it.relativeTo(projectDir).path }

        fun relativize(vararg files: File): List<String> =
            files.map { it.relativeTo(projectDir).path }

        fun performModifications() {
            for (file in projectDir.walk()) {
                if (!file.isFile) continue

                val fileWithoutExt = File(file.parentFile, file.nameWithoutExtension)

                when (file.extension) {
                    "new" -> {
                        file.copyTo(fileWithoutExt, overwrite = true)
                        file.delete()
                    }
                    "delete" -> {
                        fileWithoutExt.delete()
                        file.delete()
                    }
                }
            }
        }
    }

    class CompiledProject(val project: Project, val output: String, val resultCode: Int) {
        companion object {
            val kotlinSourcesListRegex = Regex("\\[KOTLIN\\] compile iteration: ([^\\r\\n]*)")
            val javaSourcesListRegex = Regex("\\[DEBUG\\] \\[[^\\]]*JavaCompiler\\] Compiler arguments: ([^\\r\\n]*)")
        }

        private fun getCompiledFiles(regex: Regex, output: String) = regex.findAll(output)
            .asIterable()
            .flatMap {
                it.groups[1]!!.value.split(", ")
                    .map { File(project.projectDir, it).canonicalFile }
            }

        fun getCompiledKotlinSources(output: String) = getCompiledFiles(kotlinSourcesListRegex, output)

        val compiledJavaSources: Iterable<File> by lazy {
            javaSourcesListRegex.findAll(output).asIterable().flatMap {
                it.groups[1]!!.value.split(" ").filter { it.endsWith(".java", ignoreCase = true) }.map { File(it).canonicalFile }
            }
        }
    }

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

        val env = createEnvironmentVariablesMap(options)
        val wrapperDir = prepareWrapper(wrapperVersion, env)
        val cmd = createBuildCommand(wrapperDir, params, options)

        println("<=== Test build: ${this.projectName} $cmd ===>")

        if (!projectDir.exists()) {
            setupWorkingDir()
        }

        val result = runProcess(cmd, projectDir, env, options)
        try {
            CompiledProject(this, result.output, result.exitCode).check()
        } catch (t: Throwable) {
            // to prevent duplication of output
            if (!options.forceOutputToStdout) {
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
        val arguments = mutableListOf("-Pkotlin_version=${options.kotlinVersion}")
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

    fun CompiledProject.assertSuccessful() {
        if (resultCode == 0) return

        val errors = "(?m)^.*\\[ERROR] \\[\\S+] (.*)$".toRegex().findAll(output)
        val errorMessage = buildString {
            appendln("Gradle build failed")
            appendln()
            if (errors.any()) {
                appendln("Possible errors:")
                errors.forEach { match -> appendln(match.groupValues[1]) }
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
        assertNull(regex.find(output), "Output should not contain '$regex'")
    }

    fun CompiledProject.assertNoWarnings() {
        val warnings = "w: .*".toRegex().findAll(output).map { it.groupValues[0] }

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

    fun CompiledProject.assertFileExists(path: String = ""): CompiledProject {
        assertTrue(fileInWorkingDir(path).exists(), "The file [$path] does not exist.")
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

    private fun Iterable<File>.projectRelativePaths(project: Project): Iterable<String> {
        return map { it.canonicalFile.toRelativeString(project.projectDir) }
    }

    fun CompiledProject.assertSameFiles(expected: Iterable<String>, actual: Iterable<String>, messagePrefix: String): CompiledProject {
        val expectedSet = expected.map { it.normalizePath() }.toSortedSet().joinToString("\n")
        val actualSet = actual.map { it.normalizePath() }.toSortedSet().joinToString("\n")
        Assert.assertEquals(messagePrefix, expectedSet, actualSet)
        return this
    }

    fun CompiledProject.assertContainFiles(
        expected: Iterable<String>,
        actual: Iterable<String>,
        messagePrefix: String = ""
    ): CompiledProject {
        val expectedNormalized = expected.map(::normalizePath).toSortedSet()
        val actualNormalized = actual.map(::normalizePath).toSortedSet()
        assertTrue(
            actualNormalized.containsAll(expectedNormalized),
            messagePrefix + "expected files: ${expectedNormalized.joinToString()}\n  !in actual files: ${actualNormalized.joinToString()}"
        )
        return this
    }

    fun CompiledProject.findTasksByPattern(pattern: String): Set<String> {
        return "task '($pattern)'".toRegex().findAll(output).mapTo(HashSet()) { it.groupValues[1] }
    }

    fun CompiledProject.assertTasksExecuted(tasks: Iterable<String>) {
        for (task in tasks) {
            assertContainsRegex("(Executing actions for task|Executing task) '$task'".toRegex())
        }
    }

    fun CompiledProject.assertTasksExecuted(vararg tasks: String) {
        assertTasksExecuted(tasks.toList())
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

    fun CompiledProject.assertTasksNotRealized(vararg tasks: String) {
        for (task in tasks) {
            assertNotContains("'Realize task $task'")
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

    fun CompiledProject.getOutputForTask(taskName: String): String {
        val taskOutputRegex = ("(?:\\[LIFECYCLE] \\[class org\\.gradle(?:\\.internal\\.buildevents)?\\.TaskExecutionLogger] :$taskName|" +
                "\\[org\\.gradle\\.execution\\.plan\\.DefaultPlanExecutor\\] :$taskName.*?started)" +
                "([\\s\\S]+?)" +
                "(?:Finished executing task ':$taskName'|" +
                "\\[org\\.gradle\\.execution\\.plan\\.DefaultPlanExecutor\\] :$taskName.*?completed)").toRegex()

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
        expectedSources: Iterable<String>,
        weakTesting: Boolean = false,
        output: String = this.output,
        suffix: String = ""
    ): CompiledProject {
        val messagePrefix = "Compiled Kotlin files differ${suffix}:\n  "
        val actualSources = getCompiledKotlinSources(output).projectRelativePaths(this.project)
        return if (weakTesting) {
            assertContainFiles(expectedSources, actualSources, messagePrefix)
        } else {
            assertSameFiles(expectedSources, actualSources, messagePrefix)
        }
    }

    val Project.allKotlinFiles: Iterable<File>
        get() = projectDir.allKotlinFiles()

    fun Project.projectFile(name: String): File =
        projectDir.getFileByName(name)

    fun CompiledProject.assertCompiledJavaSources(
        sources: Iterable<String>,
        weakTesting: Boolean = false
    ): CompiledProject =
        if (weakTesting)
            assertContainFiles(sources, compiledJavaSources.projectRelativePaths(this.project), "Compiled Java files differ:\n  ")
        else
            assertSameFiles(sources, compiledJavaSources.projectRelativePaths(this.project), "Compiled Java files differ:\n  ")

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

    private fun Project.createBuildCommand(wrapperDir: File, params: Array<out String>, options: BuildOptions): List<String> =
        createGradleCommand(wrapperDir, createGradleTailParameters(options, params))

    fun Project.gradleBuildScript(subproject: String? = null): File =
        listOf("build.gradle", "build.gradle.kts").mapNotNull {
            File(projectDir, subproject?.plus("/").orEmpty() + it).takeIf(File::exists)
        }.single()

    fun Project.gradleSettingsScript(): File =
        listOf("settings.gradle", "settings.gradle.kts").mapNotNull {
            File(projectDir, it).takeIf(File::exists)
        }.single()

    /**
     * @param assertionFileName path to xml with expected test results, relative to test resources root
     */
    fun CompiledProject.assertTestResults(
        @TestDataFile assertionFileName: String,
        vararg testReportNames: String
    ) {
        val projectDir = project.projectDir
        val testReportDirs = testReportNames.map { projectDir.resolve("build/test-results/$it") }

        testReportDirs.forEach {
            if (!it.isDirectory) {
                error("Test report dir $it was not created")
            }
        }

        val actualTestResults = readAndCleanupTestResults(testReportDirs, projectDir)
        val expectedTestResults = prettyPrintXml(resourcesRootFile.resolve(assertionFileName).readText())

        assertEquals(expectedTestResults, actualTestResults)
    }

    private fun readAndCleanupTestResults(testReportDirs: List<File>, projectDir: File): String {
        val files = testReportDirs
            .flatMap {
                (it.listFiles() ?: arrayOf()).filter {
                    it.isFile && it.name.endsWith(".xml")
                }
            }
            .sortedBy {
                // let containing test suite be first
                it.name.replace(".xml", ".A.xml")
            }

        val xmlString = buildString {
            appendln("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
            appendln("<results>")
            files.forEach {
                appendln(
                    it.readText()
                        .trimTrailingWhitespaces()
                        .replace(projectDir.absolutePath, "/\$PROJECT_DIR$")
                        .replace(projectDir.name, "\$PROJECT_NAME$")
                        .replace("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n", "")
                )
            }
            appendln("</results>")
        }

        val doc = SAXBuilder().build(xmlString.reader())
        val skipAttrs = setOf("timestamp", "hostname", "time", "message")
        val skipContentsOf = setOf("failure")

        fun cleanup(e: Element) {
            if (e.name in skipContentsOf) e.text = "..."
            e.attributes.forEach {
                if (it.name in skipAttrs) {
                    it.value = "..."
                }
            }

            e.children.forEach {
                cleanup(it)
            }
        }

        cleanup(doc.rootElement)
        return XMLOutputter(Format.getPrettyFormat()).outputString(doc)
    }

    private fun prettyPrintXml(uglyXml: String): String =
            XMLOutputter(Format.getPrettyFormat()).outputString(SAXBuilder().build(uglyXml.reader()))

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
            options.incremental?.let {
                add("-Pkotlin.incremental=$it")
            }
            options.incrementalJs?.let { add("-Pkotlin.incremental.js=$it") }
            options.usePreciseJavaTracking?.let { add("-Pkotlin.incremental.usePreciseJavaTracking=$it") }
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
            }

            options.parallelTasksInProject?.let {
                add("-Pkotlin.parallel.tasks.in.project=$it")
            }

            // Workaround: override a console type set in the user machine gradle.properties (since Gradle 4.3):
            add("--console=plain")
            add("-Dkotlin.daemon.ea=true")
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
