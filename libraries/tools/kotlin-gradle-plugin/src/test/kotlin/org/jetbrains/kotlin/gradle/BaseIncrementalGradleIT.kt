package org.jetbrains.kotlin.gradle

import org.gradle.api.logging.LogLevel
import org.jetbrains.kotlin.gradle.incremental.BuildStep
import org.jetbrains.kotlin.gradle.incremental.parseTestBuildLog
import org.jetbrains.kotlin.incremental.testingUtils.TouchPolicy
import org.jetbrains.kotlin.incremental.testingUtils.copyTestSources
import org.jetbrains.kotlin.incremental.testingUtils.getModificationsToPerform
import org.junit.Assume
import java.io.File
import kotlin.test.assertNotNull

abstract class BaseIncrementalGradleIT : BaseGradleIT() {

    inner class JpsTestProject(val resourcesBase: File, val relPath: String, wrapperVersion: String = "2.10", minLogLevel: LogLevel = LogLevel.DEBUG) : Project(File(relPath).name, wrapperVersion, minLogLevel) {
        override val resourcesRoot = File(resourcesBase, relPath)
        val mapWorkingToOriginalFile = hashMapOf<File, File>()

        override fun setupWorkingDir() {
            val srcDir = File(projectDir, "src")
            srcDir.mkdirs()
            val sourceMapping = copyTestSources(resourcesRoot, srcDir, filePrefix = "")
            mapWorkingToOriginalFile.putAll(sourceMapping)
            copyDirRecursively(File(resourcesRootFile, "GradleWrapper-$wrapperVersion"), projectDir)
            copyDirRecursively(File(resourcesRootFile, "incrementalGradleProject"), projectDir)
        }
    }

    fun JpsTestProject.performAndAssertBuildStages(options: BuildOptions = defaultBuildOptions(), weakTesting: Boolean = false) {
        // TODO: support multimodule tests
        if (resourcesRoot.walk().filter { it.name.equals("dependencies.txt", ignoreCase = true) }.any()) {
            Assume.assumeTrue("multimodule tests are not supported yet", false)
        }

        build("build", options = options) {
            assertSuccessful()
            assertReportExists()
        }

        val buildLogFile = resourcesRoot.listFiles { f: File -> f.name.endsWith("build.log") }?.sortedBy { it.length() }?.firstOrNull()
        assertNotNull(buildLogFile, "*build.log file not found" )

        val buildLogSteps = parseTestBuildLog(buildLogFile!!)
        val modifications = getModificationsToPerform(resourcesRoot,
                                                      moduleNames = null,
                                                      allowNoFilesWithSuffixInTestData = false,
                                                      touchPolicy = TouchPolicy.CHECKSUM)

        assert(modifications.size == buildLogSteps.size) {
            "Modifications count (${modifications.size}) != expected build log steps count (${buildLogSteps.size})"
        }

        println("<--- Expected build log size: ${buildLogSteps.size}")
        buildLogSteps.forEach {
            println("<--- Expected build log stage: ${if (it.compileSucceeded) "succeeded" else "failed"}: kotlin: ${it.compiledKotlinFiles} java: ${it.compiledJavaFiles}")
        }


        for ((modificationStep, buildLogStep) in modifications.zip(buildLogSteps)) {
            modificationStep.forEach { it.perform(projectDir, mapWorkingToOriginalFile) }
            buildAndAssertStageResults(buildLogStep, weakTesting = weakTesting)
        }
    }

    fun JpsTestProject.buildAndAssertStageResults(expected: BuildStep, options: BuildOptions = defaultBuildOptions(), weakTesting: Boolean = false) {
        build("build", options = options) {
            if (expected.compileSucceeded) {
                assertSuccessful()
                assertCompiledJavaSources(expected.compiledJavaFiles, weakTesting)
                assertCompiledKotlinSources(expected.compiledKotlinFiles, weakTesting)
            }
            else {
                assertFailed()
            }
        }
    }
}

fun isJpsTestProject(projectRoot: File): Boolean = projectRoot.listFiles { f: File -> f.name.endsWith("build.log") }?.any() ?: false


