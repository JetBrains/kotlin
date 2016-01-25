package org.jetbrains.kotlin.gradle

import com.google.common.io.Files
import org.gradle.api.logging.LogLevel
import org.junit.Assume
import java.io.File
import java.util.*
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


abstract class BaseIncrementalGradleIT : BaseGradleIT() {

    open inner class IncrementalTestProject(name: String, wrapperVersion: String = "2.4", minLogLevel: LogLevel = LogLevel.DEBUG) : Project(name, wrapperVersion, minLogLevel) {
        var modificationStage: Int = 1
    }

    inner class JpsTestProject(val resourcesBase: File, val relPath: String, wrapperVersion: String = "1.6", minLogLevel: LogLevel = LogLevel.DEBUG) : IncrementalTestProject(File(relPath).name, wrapperVersion, minLogLevel) {
        override val resourcesRoot = File(resourcesBase, relPath)

        override fun setupWorkingDir() {
            val srcDir = File(projectDir, "src")
            srcDir.mkdirs()
            resourcesRoot.walk()
                    .filter { it.isFile && (it.name.endsWith(".kt") || it.name.endsWith(".java")) }
                    .forEach { Files.copy(it, File(srcDir, it.name)) }
            copyDirRecursively(File(resourcesRootFile, "GradleWrapper-$wrapperVersion"), projectDir)
            File(projectDir, "build.gradle").writeText("""
buildscript {
  repositories {
    maven {
        url 'file://' + pathToKotlinPlugin
    }
  }
  dependencies {
    classpath 'org.jetbrains.kotlin:kotlin-gradle-plugin:0.1-SNAPSHOT'
  }
}

apply plugin: "kotlin"

sourceSets {
  main {
     kotlin {
        srcDir 'src'
     }
     java {
        srcDir 'src'
     }
  }
}

repositories {
  maven {
     url 'file://' + pathToKotlinPlugin
  }
}
            """)
        }
    }

    fun IncrementalTestProject.modify(runStage: Int? = null) {
        // TODO: multimodule support
        val projectSrcDir = File(File(workingDir, projectName), "src")
        assertTrue(projectSrcDir.exists())
        val actualStage = runStage ?: modificationStage

        println("<--- Modify stage: ${runStage?.toString() ?: "single"}")

        fun resource2project(f: File) = File(projectSrcDir, f.toRelativeString(resourcesRoot))

        resourcesRoot.walk().filter { it.isFile }.forEach {
            val nameParts = it.name.split(".")
            if (nameParts.size > 2) {
                val (fileStage, hasStage) = nameParts.last().toIntOr(0)
                if (!hasStage || fileStage == actualStage) {
                    val orig = File(resource2project(it.parentFile), nameParts.dropLast(if (hasStage) 2 else 1).joinToString("."))
                    when (if (hasStage) nameParts[nameParts.size - 2] else nameParts.last()) {
                        "touch" -> {
                            assert(orig.exists())
                            orig.setLastModified(Date().time)
                            println("<--- Modify: touch $orig")
                        }
                        "new" -> {
                            it.copyTo(orig, overwrite = true)
                            orig.setLastModified(Date().time)
                            println("<--- Modify: new $orig from $it")
                        }
                        "delete" -> {
                            assert(orig.exists())
                            orig.delete()
                            println("<--- Modify: delete $orig")
                        }
                    }
                }
            }
        }

        modificationStage = actualStage + 1
    }

    class StageResults(val stage: Int, val compiledKotlinFiles: HashSet<String> = hashSetOf(), val compiledJavaFiles: HashSet<String> = hashSetOf(), var compileSucceeded: Boolean = true)

    fun parseTestBuildLog(file: File): List<StageResults> {
        class StagedLines(val stage: Int, val line: String)

        return file.readLines()
                .map { if (it.startsWith("========== Step")) "" else it }
                .fold(arrayListOf<StagedLines>()) { slines, line ->
                    val (curStage, prevWasBlank) = slines.lastOrNull()?.let{ Pair(it.stage, it.line.isBlank()) } ?: Pair(0, false)
                    slines.add(StagedLines(curStage + if (line.isBlank() && prevWasBlank) 1 else 0, line))
                    slines
                }
                .fold(arrayListOf<StageResults>()) { res, sline ->
                    // for lazy creation of the node
                    fun curStageResults(): StageResults {
                        if (res.isEmpty() || sline.stage > res.last().stage) {
                            res.add(StageResults(sline.stage))
                        }
                        return res.last()
                    }

                    when {
                        sline.line.endsWith(".java", ignoreCase = true) -> curStageResults().compiledJavaFiles.add(sline.line)
                        sline.line.endsWith(".kt", ignoreCase = true) -> curStageResults().compiledKotlinFiles.add(sline.line)
                        sline.line.equals("COMPILATION FAILED", ignoreCase = true) -> curStageResults().compileSucceeded = false
                    }
                    res
                }
    }


    fun IncrementalTestProject.performAndAssertBuildStages(options: BuildOptions = defaultBuildOptions()) {

        val checkKnown = testIsKnownJpsTestProject(resourcesRoot)
        Assume.assumeTrue(checkKnown.second ?: "", checkKnown.first)

        build("build", options = options) {
            assertSuccessful()
            assertReportExists()
        }

        val buildLogFile = resourcesRoot.listFiles { f: File -> f.name.endsWith("build.log") }?.sortedBy { it.length() }?.firstOrNull()
        assertNotNull(buildLogFile, "*build.log file not found" )

        val buildLog = parseTestBuildLog(buildLogFile!!)
        assertTrue(buildLog.any())

        println("<--- Build log size: ${buildLog.size}")
        buildLog.forEach {
            println("<--- Build log stage: ${if (it.compileSucceeded) "succeeded" else "failed"}: kotlin: ${it.compiledKotlinFiles} java: ${it.compiledJavaFiles}")
        }

        if (buildLog.size == 1) {
            modify()
            buildAndAssertStageResults(buildLog.first())
        }
        else {
            buildLog.forEachIndexed { stage, stageResults ->
                modify(stage + 1)
                buildAndAssertStageResults(stageResults)
            }
        }
    }

    fun IncrementalTestProject.buildAndAssertStageResults(expected: StageResults, options: BuildOptions = defaultBuildOptions()) {
        build("build", options = options) {
            if (expected.compileSucceeded) {
                assertSuccessful()
                assertCompiledJavaSources(expected.compiledJavaFiles)
                assertCompiledKotlinSources(expected.compiledKotlinFiles)
            }
            else {
                assertFailed()
            }
        }
    }
}


private val knownExtensions = arrayListOf("kt", "java")
private val knownModifyExtensions = arrayListOf("new", "touch", "delete")

private fun String.toIntOr(defaultVal: Int): Pair<Int, Boolean> {
    try {
        return Pair(toInt(), true)
    }
    catch (e: NumberFormatException) {
        return Pair(defaultVal, false)
    }
}

fun isJpsTestProject(projectRoot: File): Boolean = projectRoot.listFiles { f: File -> f.name.endsWith("build.log") }?.any() ?: false

fun testIsKnownJpsTestProject(projectRoot: File): Pair<Boolean, String?> {
    var hasKnownSources = false
    projectRoot.walk().filter { it.isFile }.forEach {
        if (it.name.equals("dependencies.txt", ignoreCase = true))
            return@testIsKnownJpsTestProject Pair(false, "multimodule tests are not supported yet")
        val nameParts = it.name.split(".")
        if (nameParts.size > 1) {
            val (fileStage, hasStage) = nameParts.last().toIntOr(0)
            val modifyExt = nameParts[nameParts.size - (if (hasStage) 2 else 1)]
            val ext = nameParts[nameParts.size - (if (hasStage) 3 else 2)]
            if (modifyExt in knownModifyExtensions && ext !in knownExtensions)
                return@testIsKnownJpsTestProject Pair(false, "unknown staged file ${it.name}")
        }
        if (!hasKnownSources && it.extension in knownExtensions) {
            hasKnownSources = true
        }
    }
    return if (hasKnownSources) Pair(true, null)
    else Pair(false, "no known sources found")
}


