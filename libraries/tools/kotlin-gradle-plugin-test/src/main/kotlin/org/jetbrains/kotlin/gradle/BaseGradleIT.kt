package org.jetbrains.kotlin.gradle

import com.google.common.io.Files
import java.io.File
import java.io.InputStream
import java.util.Scanner
import org.junit.Before
import org.junit.After
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.fail

open class BaseGradleIT(resourcesRoot: String = "src/test/resources") {

    private val resourcesRootFile = File(resourcesRoot)
    private var workingDir = File(".")

    Before fun setUp() {
        workingDir = Files.createTempDir()
    }

    After fun tearDown() {
        deleteRecursively(workingDir)
    }

    class Project(val projectName: String, val wrapperVersion: String = "1.4")

    class CompiledProject(val project: Project, val output: String, val resultCode: Int)

    fun Project.build(vararg tasks: String, check: CompiledProject.() -> Unit) {
        copyRecursively(File(resourcesRootFile, "testProject/$projectName"), workingDir)
        val projectDir = File(workingDir, projectName)
        copyDirRecursively(File(resourcesRootFile, "GradleWrapper-$wrapperVersion"), projectDir)
        val cmd = createCommand(tasks)
        val process = createProcess(cmd, projectDir)

        val (output, resultCode) = readOutput(process)
        CompiledProject(this, output, resultCode).check()
    }

    fun CompiledProject.assertSuccessful(): CompiledProject {
        assertEquals(resultCode, 0, "Gradle build failed")
        return this
    }

    fun CompiledProject.assertContains(vararg expected: String): CompiledProject {
        for (str in expected) {
            assertTrue(output.contains(str), "Should contain '$str', actual output: $output")
        }
        return this
    }

    fun CompiledProject.assertReportExists(pathToReport: String = ""): CompiledProject {
        assertTrue(File(File(workingDir, project.projectName), pathToReport).exists(), "The report [$pathToReport] does not exist.")
        return this
    }

    private fun createCommand(params: Array<String>): List<String> {
        val pathToKotlinPlugin = "-PpathToKotlinPlugin=" + File("local-repo").getAbsolutePath()
        val tailParameters = params + listOf(pathToKotlinPlugin, "--no-daemon", "--debug")

        return if (isWindows())
            listOf("cmd", "/C", "gradlew.bat") + tailParameters
         else
            listOf("/bin/bash", "./gradlew") + tailParameters
    }

    private fun isWindows(): Boolean {
        return System.getProperty("os.name")!!.contains("Windows")
    }

    private fun createProcess(cmd: List<String>, projectDir: File): Process {
        val builder = ProcessBuilder(cmd)
        builder.directory(projectDir)
        builder.redirectErrorStream(true)
        return builder.start()
    }

    private fun readOutput(process: Process): Pair<String, Int> {
        fun InputStream.readFully(): String {
            val text = reader().readText()
            close()
            return text
        }

        val stdout = process.getInputStream()!!.readFully()
        System.out.println(stdout)
        val stderr = process.getErrorStream()!!.readFully()
        System.err.println(stderr)

        val result = process.waitFor()
        return stdout to result
    }

    fun copyRecursively(source: File, target: File) {
        assertTrue(target.isDirectory())
        val targetFile = File(target, source.getName())
        if (source.isDirectory()) {
            targetFile.mkdir()
            source.listFiles()?.forEach { copyRecursively(it, targetFile) }
        } else {
            Files.copy(source, targetFile)
        }
    }

    fun copyDirRecursively(source: File, target: File) {
        assertTrue(source.isDirectory())
        assertTrue(target.isDirectory())
        source.listFiles()?.forEach { copyRecursively(it, target) }
    }

    fun deleteRecursively(f: File): Unit {
        if (f.isDirectory()) {
            f.listFiles()?.forEach { deleteRecursively(it) }
            val fileList = f.listFiles()
            if (fileList != null) {
                assertTrue(fileList.isEmpty())
            } else {
                fail("Error listing directory content")
            }
        }
        f.delete()
    }
}