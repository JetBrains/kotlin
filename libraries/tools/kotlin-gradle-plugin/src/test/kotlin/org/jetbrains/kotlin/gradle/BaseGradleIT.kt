package org.jetbrains.kotlin.gradle

import com.google.common.io.Files
import com.intellij.openapi.util.io.FileUtil
import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.util.createGradleCommand
import org.jetbrains.kotlin.gradle.util.runProcess
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import java.io.File
import kotlin.test.*

private val SYSTEM_LINE_SEPARATOR = System.getProperty("line.separator")

abstract class BaseGradleIT {

    protected var workingDir = File(".")

    protected open fun defaultBuildOptions(): BuildOptions = BuildOptions(withDaemon = false)

    @Before
    fun setUp() {
        workingDir = Files.createTempDir()
    }

    @After
    fun tearDown() {
        deleteRecursively(workingDir)
    }

    companion object {

        protected val ranDaemonVersions = hashMapOf<String, Int>()
        val resourcesRootFile = File("src/test/resources")
        val MAX_DAEMON_RUNS = 30

        @AfterClass
        @JvmStatic
        @Synchronized
        @Suppress("unused")
        fun tearDownAll() {
            ranDaemonVersions.keys.forEach { stopDaemon(it) }
            ranDaemonVersions.clear()
        }

        fun stopDaemon(ver: String) {
            println("Stopping gradle daemon v$ver")
            val wrapperDir = File(resourcesRootFile, "GradleWrapper-$ver")
            val cmd = createGradleCommand(arrayListOf("-stop"))
            val result = runProcess(cmd, wrapperDir)
            assert(result.isSuccessful) { "Could not stop daemon: $result" }
        }

        @Synchronized
        fun prepareDaemon(version: String) {
            val useCount = ranDaemonVersions.get(version)
            if (useCount == null || useCount > MAX_DAEMON_RUNS) {
                stopDaemon(version)
                ranDaemonVersions.put(version, 1)
            }
            else {
                ranDaemonVersions.put(version, useCount + 1)
            }
        }
    }

    // the second parameter is for using with ToolingAPI, that do not like --daemon/--no-daemon  options at all
    data class BuildOptions(
            val withDaemon: Boolean = false,
            val daemonOptionSupported: Boolean = true,
            val incremental: Boolean? = null,
            val androidHome: File? = null,
            val androidGradlePluginVersion: String? = null)

    open inner class Project(val projectName: String, val wrapperVersion: String = "1.4", val minLogLevel: LogLevel = LogLevel.DEBUG) {
        open val resourcesRoot = File(resourcesRootFile, "testProject/$projectName")
        val projectDir = File(workingDir.canonicalFile, projectName)

        open fun setupWorkingDir() {
            copyRecursively(this.resourcesRoot, workingDir)
            copyDirRecursively(File(resourcesRootFile, "GradleWrapper-$wrapperVersion"), projectDir)
        }

        fun relativize(files: Iterable<File>): List<String> =
                files.map { it.relativeTo(projectDir).path }

        fun relativize(vararg files: File): List<String> =
                files.map { it.relativeTo(projectDir).path }

        fun relativizeToSubproject(subproject: String, vararg files: File): List<String> {
            val subprojectSir = File(projectDir, subproject)
            return files.map { it.relativeTo(subprojectSir).path }
        }
    }

    class CompiledProject(val project: Project, val output: String, val resultCode: Int) {
        companion object {
            val kotlinSourcesListRegex = Regex("\\[KOTLIN\\] compile iteration: ([^\\r\\n]*)")
            val javaSourcesListRegex = Regex("\\[DEBUG\\] \\[[^\\]]*JavaCompiler\\] Compiler arguments: ([^\\r\\n]*)")
        }

        val compiledKotlinSources: Iterable<File> by lazy { kotlinSourcesListRegex.findAll(output).asIterable().flatMap { it.groups[1]!!.value.split(", ").map { File(project.projectDir, it).canonicalFile } } }
        val compiledJavaSources: Iterable<File> by lazy { javaSourcesListRegex.findAll(output).asIterable().flatMap { it.groups[1]!!.value.split(" ").filter { it.endsWith(".java", ignoreCase = true) }.map { File(it).canonicalFile } } }
    }

    fun Project.build(vararg params: String, options: BuildOptions = defaultBuildOptions(), check: CompiledProject.() -> Unit) {
        val cmd = createBuildCommand(params, options)
        val env = createEnvironmentVariablesMap(options)

        if (options.withDaemon) {
            prepareDaemon(wrapperVersion)
        }

        println("<=== Test build: ${this.projectName} $cmd ===>")

        val projectDir = File(workingDir, projectName)
        if (!projectDir.exists()) {
            setupWorkingDir()
        }

        val result = runProcess(cmd, projectDir, env)
        CompiledProject(this, result.output, result.exitCode).check()
    }

    fun CompiledProject.assertSuccessful(): CompiledProject {
        assertEquals(0, resultCode, "Gradle build failed")
        return this
    }

    fun CompiledProject.assertFailed(): CompiledProject {
        assertNotEquals(0, resultCode, "Expected that Gradle build failed")
        return this
    }

    fun CompiledProject.assertContains(vararg expected: String): CompiledProject {
        for (str in expected) {
            assertTrue(output.contains(str.normalize()), "Output should contain '$str'")
        }
        return this
    }

    fun CompiledProject.assertNotContains(vararg expected: String): CompiledProject {
        for (str in expected) {
            assertFalse(output.contains(str.normalize()), "Output should not contain '$str'")
        }
        return this
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
//        val projectDir = File(workingDir.canonicalFile, project.projectName)
        return map { it.canonicalFile.toRelativeString(project.projectDir) }
    }

    fun CompiledProject.assertSameFiles(expected: Iterable<String>, actual: Iterable<String>, messagePrefix: String = ""): CompiledProject {
        val expectedSet = expected.toSortedSet()
        val actualSet = actual.toSortedSet()
        assertTrue(actualSet == expectedSet, messagePrefix + "expected files: ${expectedSet.joinToString()}\n  != actual files: ${actualSet.joinToString()}")
        return this
    }

    fun CompiledProject.assertContainFiles(expected: Iterable<String>, actual: Iterable<String>, messagePrefix: String = ""): CompiledProject {
        val expectedNormalized = expected.map(FileUtil::normalize).toSortedSet()
        val actualNormalized = actual.map(FileUtil::normalize).toSortedSet()
        assertTrue(actualNormalized.containsAll(expectedNormalized), messagePrefix + "expected files: ${expectedNormalized.joinToString()}\n  !in actual files: ${actualNormalized.joinToString()}")
        return this
    }

    fun CompiledProject.assertCompiledKotlinSources(sources: Iterable<String>, weakTesting: Boolean = false): CompiledProject =
            if (weakTesting)
                assertContainFiles(sources, compiledKotlinSources.projectRelativePaths(this.project), "Compiled Kotlin files differ:\n  ")
            else
                assertSameFiles(sources, compiledKotlinSources.projectRelativePaths(this.project), "Compiled Kotlin files differ:\n  ")

    fun CompiledProject.assertCompiledJavaSources(sources: Iterable<String>, weakTesting: Boolean = false): CompiledProject =
            if (weakTesting)
                assertContainFiles(sources, compiledJavaSources.projectRelativePaths(this.project), "Compiled Java files differ:\n  ")
            else
                assertSameFiles(sources, compiledJavaSources.projectRelativePaths(this.project), "Compiled Java files differ:\n  ")

    private fun Project.createBuildCommand(params: Array<out String>, options: BuildOptions): List<String> =
            createGradleCommand(createGradleTailParameters(options, params))

    private fun Project.createGradleTailParameters(options: BuildOptions, params: Array<out String> = arrayOf()): List<String> =
            params.toMutableList().apply {
                add("--stacktrace")
                add("--${minLogLevel.name.toLowerCase()}")
                if (options.daemonOptionSupported) {
                    add(if (options.withDaemon) "--daemon" else "--no-daemon")
                }

                add("-PpathToKotlinPlugin=" + File("local-repo").absolutePath)
                options.incremental?.let { add("-Pkotlin.incremental=$it") }
                options.androidGradlePluginVersion?.let { add("-PandroidToolsVersion=$it")}
            }

    private fun Project.createEnvironmentVariablesMap(options: BuildOptions): Map<String, String> =
            hashMapOf<String, String>().apply {
                val sdkDir = options.androidHome
                if (sdkDir != null) {
                    sdkDir.parentFile.mkdirs()
                    put("ANDROID_HOME", sdkDir.canonicalPath)
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
            Files.copy(source, targetFile)
        }
    }

    fun copyDirRecursively(source: File, target: File) {
        assertTrue(source.isDirectory)
        assertTrue(target.isDirectory)
        source.listFiles()?.forEach { copyRecursively(it, target) }
    }

    fun deleteRecursively(f: File): Unit {
        if (f.isDirectory) {
            f.listFiles()?.forEach { deleteRecursively(it) }
            val fileList = f.listFiles()
            if (fileList != null) {
                if (!fileList.isEmpty()) {
                    fail("Expected $f to be empty but it has files: ${fileList.joinToString { it.name }}")
                }
            } else {
                fail("Error listing directory content")
            }
        }
        f.delete()
    }
}