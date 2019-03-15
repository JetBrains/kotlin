/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.incapt.IncrementalProcessor
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class KaptIncrementalWithIsolatingApt : KaptIncrementalIT() {

    override fun getProject() =
        Project(
            "kaptIncrementalCompilationProject",
            GradleVersionRequired.None
        ).apply {
            setupIncrementalAptProject("ISOLATING")
        }

    override fun defaultBuildOptions(): BuildOptions =
        super.defaultBuildOptions().copy(
            incremental = true,
            kaptOptions = KaptOptions(
                verbose = true,
                useWorkers = true,
                incrementalKapt = true,
                includeCompileClasspath = false
            )
        )

    @Test
    fun testIncrementalChanges() {
        val project = getProject()

        var aptTimestamp = 0L

        project.build("clean", "build") {
            assertSuccessful()

            val classpathHistory =
                fileInWorkingDir("build/kotlin/kaptGenerateStubsKotlin/classpath-fq-history").listFiles().asList().single()
            val stubsTimestamp = classpathHistory.name.toLong()

            aptTimestamp = fileInWorkingDir("build/tmp/kapt3/incApCache/main/last-build-ts.bin").readText().toLong()
            assertTrue(stubsTimestamp < aptTimestamp)
        }

        project.projectFile("useB.kt").modify { current -> "$current\nfun otherFunction() {}" }
        project.build("build") {
            assertSuccessful()

            val newAptTimestamp = fileInWorkingDir("build/tmp/kapt3/incApCache/main/last-build-ts.bin").readText().toLong()
            assertTrue(aptTimestamp < newAptTimestamp)

            assertEquals(setOf(fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/UseBKt.java").absolutePath), getProcessedSources(output))
        }

        project.projectFile("B.kt").modify { current ->
            val lastBrace = current.lastIndexOf("}")
            current.substring(0, lastBrace) + "fun anotherFun() {}\n }"
        }
        project.build("build") {
            assertSuccessful()
            assertEquals(
                setOf(
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/B.java").absolutePath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/UseBKt.java").absolutePath
                ),
                getProcessedSources(output)
            )
        }
    }
}

private const val patternApt = "Processing java sources with annotation processors:"
fun getProcessedSources(output: String): Set<String> {
    val logging = output.lines().single { it.contains(patternApt) }
    val indexOf = logging.indexOf(patternApt) + patternApt.length
    return logging.drop(indexOf).split(",").map { it.trim() }.toSet()
}

fun BaseGradleIT.Project.setupIncrementalAptProject(procType: String) {
    setupWorkingDir()
    val buildFile = projectDir.resolve("build.gradle")
    val content = buildFile.readText()
    val processorPath = projectDir.resolve("incrementalProcessor.jar")

    ZipOutputStream(processorPath.outputStream()).use {
        val path = IncrementalProcessor::class.java.name.replace(".", "/") + ".class"
        val inputStream = IncrementalProcessor::class.java.classLoader.getResourceAsStream(path)
        it.putNextEntry(ZipEntry(path))
        it.write(inputStream.readBytes())
        it.closeEntry()
        it.putNextEntry(ZipEntry("META-INF/gradle/incremental.annotation.processors"))
        it.write("${IncrementalProcessor::class.java.name},$procType".toByteArray())
        it.closeEntry()
        it.putNextEntry(ZipEntry("META-INF/services/javax.annotation.processing.Processor"))
        it.write(IncrementalProcessor::class.java.name.toByteArray())
        it.closeEntry()
    }

    val updatedContent = content.replace(
        Regex("^\\s*kapt\\s\"org\\.jetbrain.*$", RegexOption.MULTILINE),
        "    kapt files(\"${processorPath.invariantSeparatorsPath}\")"
    )
    buildFile.writeText(updatedContent)
}
