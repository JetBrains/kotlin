package org.jetbrains.kotlin.gradle

import com.google.common.io.Files
import org.gradle.api.logging.LogLevel
import org.junit.After
import org.junit.AfterClass
import org.junit.Before
import java.io.File
import java.io.InputStream
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.fail

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
        protected val ranDaemonVersions = hashSetOf<String>()
        val resourcesRootFile = File("src/test/resources")

        @AfterClass
        @JvmStatic
        @Suppress("unused")
        fun tearDownAll() {
            ranDaemonVersions.forEach {
                println("Stopping gradle daemon v$it")
                stopDaemon(it)
            }
            ranDaemonVersions.clear()
        }

        fun createGradleCommand(tailParameters: List<String>): List<String> {
            return if (isWindows())
                listOf("cmd", "/C", "gradlew.bat") + tailParameters
            else
                listOf("/bin/bash", "./gradlew") + tailParameters
        }

        fun isWindows(): Boolean {
            return System.getProperty("os.name")!!.contains("Windows")
        }

        fun stopDaemon(ver: String) {
            val wrapperDir = File(resourcesRootFile, "GradleWrapper-$ver")
            val cmd = createGradleCommand(arrayListOf("-stop"))
            createProcess(cmd, wrapperDir)
        }

        fun createProcess(cmd: List<String>, projectDir: File): Process {
            val builder = ProcessBuilder(cmd)
            builder.directory(projectDir)
            builder.redirectErrorStream(true)
            return builder.start()
        }

        fun prepareDaemon(version: String) {
            if (version !in ranDaemonVersions) {
                stopDaemon(version)
                ranDaemonVersions.add(version)
            }
        }
    }

    // the second parameter is for using with ToolingAPI, that do not like --daemon/--no-daemon  options at all
    data class BuildOptions(val withDaemon: Boolean = false, val daemonOptionSupported: Boolean = true)

    open inner class Project(val projectName: String, val wrapperVersion: String = "1.4", val minLogLevel: LogLevel = LogLevel.DEBUG) {
        open val resourcesRoot = File(resourcesRootFile, "testProject/$projectName")
        val projectDir = File(workingDir.canonicalFile, projectName)

        open fun setupWorkingDir() {
            copyRecursively(this.resourcesRoot, workingDir)
            copyDirRecursively(File(resourcesRootFile, "GradleWrapper-$wrapperVersion"), projectDir)
        }
    }

    class CompiledProject(val project: Project, val output: String, val resultCode: Int) {
        companion object {
            val kotlinSourcesListRegex = Regex("\\[KOTLIN\\] compile iteration: ([^\\r\\n]*)")
            val javaSourcesListRegex = Regex("\\[DEBUG\\] \\[[^\\]]*JavaCompiler\\] Compiler arguments: ([^\\r\\n]*)")
        }
        val compiledKotlinSources : Iterable<File> by lazy { kotlinSourcesListRegex.findAll(output).asIterable().flatMap { it.groups[1]!!.value.split(", ").map { File(project.projectDir, it).canonicalFile } } }
        val compiledJavaSources : Iterable<File> by lazy { javaSourcesListRegex.findAll(output).asIterable().flatMap { it.groups[1]!!.value.split(" ").filter { it.endsWith(".java", ignoreCase = true) }.map { File(it).canonicalFile } } }
    }

    fun Project.build(vararg tasks: String, options: BuildOptions = defaultBuildOptions(), check: CompiledProject.() -> Unit) {
        val cmd = createBuildCommand(tasks, options)

        if (options.withDaemon) {
            prepareDaemon(wrapperVersion)
        }

        println("<=== Test build: ${this.projectName} $cmd ===>")

        runAndCheck(cmd, check)
    }


    fun Project.stopDaemon(check: CompiledProject.() -> Unit) {
        val cmd = createGradleCommand(arrayListOf("-stop"))
        println("<=== Stop daemon: $cmd ===>")

        runAndCheck(cmd, check)
    }

    private fun Project.runAndCheck(cmd: List<String>, check: CompiledProject.() -> Unit) {
        val projectDir = File(workingDir, projectName)
        if (!projectDir.exists())
            setupWorkingDir()

        val process = createProcess(cmd, projectDir)

        val (output, resultCode) = readOutput(process)
        CompiledProject(this, output, resultCode).check()
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
            assertTrue(output.contains(str.normalize()), "Should contain '$str', actual output: $output")
        }
        return this
    }

    fun CompiledProject.assertNotContains(vararg expected: String): CompiledProject {
        for (str in expected) {
            assertFalse(output.contains(str.normalize()), "Should not contain '$str', actual output: $output")
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
        assertTrue(actualSet == expectedSet, messagePrefix + "expected files: ${expectedSet.joinToString()}\n  actual files: ${actualSet.joinToString()}")
        return this
    }

    fun CompiledProject.assertContainFiles(expected: Iterable<String>, actual: Iterable<String>, messagePrefix: String = ""): CompiledProject {
        val expectedSet = expected.toSortedSet()
        assertTrue(expectedSet.containsAll(actual.toList()), messagePrefix + "expected files: ${expectedSet.joinToString()}\n  actual files: ${actual.toSortedSet().joinToString()}")
        return this
    }

    fun CompiledProject.assertCompiledKotlinSources(sources: Iterable<String>): CompiledProject = assertSameFiles(sources, compiledKotlinSources.projectRelativePaths(this.project), "Compiled Kotlin files differ:\n  ")
    fun CompiledProject.assertCompiledKotlinSources(vararg sources: String): CompiledProject = assertCompiledKotlinSources(sources.asIterable())

    fun CompiledProject.assertCompiledKotlinSourcesContain(vararg sources: String): CompiledProject = assertContainFiles(sources.asIterable(), compiledKotlinSources.projectRelativePaths(this.project), "Compiled Kotlin files differ:\n  ")

    fun CompiledProject.assertCompiledJavaSources(sources: Iterable<String>): CompiledProject = assertSameFiles(sources, compiledJavaSources.projectRelativePaths(this.project), "Compiled Java files differ:\n  ")
    fun CompiledProject.assertCompiledJavaSources(vararg sources: String): CompiledProject = assertCompiledJavaSources(sources.asIterable())

    fun CompiledProject.assertCompiledJavaSourcesContain(vararg sources: String): CompiledProject = assertContainFiles(sources.asIterable(), compiledJavaSources.projectRelativePaths(this.project), "Compiled Java files differ:\n  ")


    private fun Project.createBuildCommand(params: Array<out String>, options: BuildOptions): List<String> =
            createGradleCommand(createGradleTailParameters(options, params))

    protected fun Project.createGradleTailParameters(options: BuildOptions, params: Array<out String> = arrayOf()): List<String> =
            params.asList() +
                    listOf("-PpathToKotlinPlugin=" + File("local-repo").absolutePath,
                            if (options.daemonOptionSupported)
                                if (options.withDaemon) "--daemon"
                                else "--no-daemon"
                            else null,
                            "--stacktrace",
                            "--${minLogLevel.name.toLowerCase()}",
                            "-Pkotlin.gradle.test=true")
                            .filterNotNull()

    private fun String.normalize() = this.lineSequence().joinToString(SYSTEM_LINE_SEPARATOR)

    private fun readOutput(process: Process): Pair<String, Int> {
        fun InputStream.readFully(): String {
            val text = reader().readText()
            close()
            return text
        }

        val stdout = process.inputStream!!.readFully()
        System.out.println(stdout)
        val stderr = process.errorStream!!.readFully()
        System.err.println(stderr)

        val result = process.waitFor()
        return stdout to result
    }

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