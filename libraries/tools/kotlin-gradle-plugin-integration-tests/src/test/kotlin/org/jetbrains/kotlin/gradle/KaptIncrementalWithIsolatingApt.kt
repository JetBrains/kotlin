/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.incapt.IncrementalProcessor
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Assert.assertEquals
import org.junit.Assume
import org.junit.Test
import test.kt33617.MyClass
import java.io.File
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

        project.build("clean", "build") {
            assertSuccessful()
        }

        project.projectFile("useB.kt").modify { current -> "$current\nfun otherFunction() {}" }
        project.build("build") {
            assertSuccessful()

            assertEquals(
                setOf(
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/UseBKt.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath
                ), getProcessedSources(output)
            )
        }

        project.projectFile("B.kt").modify { current ->
            val lastBrace = current.lastIndexOf("}")
            current.substring(0, lastBrace) + "fun anotherFun() {}\n }"
        }
        project.build("build") {
            assertSuccessful()
            assertEquals(
                setOf(
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/B.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/bar/UseBKt.java").canonicalPath,
                    fileInWorkingDir("build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath
                ),
                getProcessedSources(output)
            )
        }
    }

    @Test
    fun testChangingAnnotationProcessorClasspath() {
        val project = getProject()

        project.build("clean", "build") {
            assertSuccessful()
        }

        project.gradleBuildScript().appendText(
            """
            
            dependencies {
                kapt 'com.google.guava:guava:12.0'
            }
        """.trimIndent()
        )
        project.build("build") {
            assertSuccessful()
            assertContains("Unable to use existing data, re-initializing classpath information for KAPT.")
        }
    }

    @Test
    fun testUnchangedAnnotationProcessorClasspathButContentChanged() {
        val project = getProject()
        val processorJar = project.projectDir.resolve("processor.jar").also {
            ZipOutputStream(it.outputStream()).use {
                // create an empty jar
            }
        }
        project.gradleBuildScript().appendText(
            """
            
            dependencies {
                kapt files("processor.jar")
            }
        """.trimIndent()
        )

        project.build("clean", "build") {
            assertSuccessful()
        }

        ZipOutputStream(processorJar.outputStream()).use {
            it.putNextEntry(ZipEntry("resource.txt"))
            it.closeEntry()
        }
        project.build("build") {
            assertSuccessful()
            assertContains("Unable to use existing data, re-initializing classpath information for KAPT.")
        }
    }

    @Test
    fun testNonIncrementalWithUnrecognizedInputs() {
        val project = getProject()

        val additionalInputs = project.projectDir.resolve("additionalInputs").also { it.mkdirs() }
        project.gradleBuildScript().appendText(
            """
            
            tasks.whenTaskAdded {
                if (it.name == "kaptKotlin") {
                  it.getInputs().files("${additionalInputs.invariantSeparatorsPath}")
                }
            }
        """.trimIndent()
        )

        project.build("clean", "build") {
            assertSuccessful()
        }

        additionalInputs.resolve("layout.xml").createNewFile()
        project.build("build") {
            assertSuccessful()
            assertContains("Incremental annotation processing (apt mode): false")
        }
    }

    /** Regression test for https://youtrack.jetbrains.com/issue/KT-33617. */
    @Test
    fun testSourcesInCompileClasspathJars() {
        val javaHome = File(System.getProperty("jdk9Home")!!)
        Assume.assumeTrue("JDK 9 isn't available", javaHome.isDirectory)
        val options = defaultBuildOptions().copy(javaHome = javaHome)

        val project = getProject()
        // create jar with .class and .java file for the same type
        ZipOutputStream(project.projectDir.resolve("lib-with-sources.jar").outputStream()).use {
            it.putNextEntry(ZipEntry("test/kt33617/MyClass.class"))
            MyClass::class.java.classLoader.getResourceAsStream("test/kt33617/MyClass.class").use { input ->
                it.write(input!!.readBytes())
            }
            it.closeEntry()

            it.putNextEntry(ZipEntry("test/kt33617/MyClass.java"))
            it.write(
                """
                package test.kt33617;
                public class MyClass {}
            """.trimIndent().toByteArray(Charsets.UTF_8)
            )
            it.closeEntry()
        }
        project.gradleBuildScript().appendText(
            """
                
            dependencies {
                implementation files('lib-with-sources.jar')
            }
        """.trimIndent()
        )
        project.projectFile("useB.kt").modify { current -> "$current\nfun otherFunction(param: test.kt33617.MyClass) {}" }

        project.build("clean", "kaptKotlin", options = options) {
            assertSuccessful()
        }
    }
}

private const val patternApt = "Processing java sources with annotation processors:"
fun getProcessedSources(output: String): Set<String> {
    val logging = output.lines().single { it.contains(patternApt) }
    val indexOf = logging.indexOf(patternApt) + patternApt.length
    return logging.drop(indexOf).split(",").map { it.trim() }.filter { !it.isEmpty() }.toSet()
}

fun BaseGradleIT.Project.setupIncrementalAptProject(
    procType: String,
    buildFile: File = projectDir.resolve("build.gradle"),
    procClass: Class<*> = IncrementalProcessor::class.java
) {
    setupIncrementalAptProject(procType to procClass, buildFile = buildFile)
}

fun BaseGradleIT.Project.setupIncrementalAptProject(
    vararg processors: Pair<String, Class<*>>,
    buildFile: File = projectDir.resolve("build.gradle")
) {
    setupWorkingDir()
    val content = buildFile.readText()
    val processorPath = generateProcessor(*processors)

    val updatedContent = content.replace(
        Regex("^\\s*kapt\\s\"org\\.jetbrains\\.kotlin.*$", RegexOption.MULTILINE),
        "    kapt files(\"${processorPath.invariantSeparatorsPath}\")"
    )
    buildFile.writeText(updatedContent)
}

fun BaseGradleIT.Project.generateProcessor(vararg processors: Pair<String, Class<*>>): File {
    val processorPath = projectDir.resolve("incrementalProcessor.jar")

    ZipOutputStream(processorPath.outputStream()).use {
        for ((_, procClass) in processors) {
            val path = procClass.name.replace(".", "/") + ".class"
            procClass.classLoader.getResourceAsStream(path).use { inputStream ->
                it.putNextEntry(ZipEntry(path))
                it.write(inputStream.readBytes())
                it.closeEntry()
            }
        }
        it.putNextEntry(ZipEntry("META-INF/gradle/incremental.annotation.processors"))
        it.write(processors.joinToString("\n") { (procType, procClass) ->
            "${procClass.name},$procType"
        }.toByteArray())
        it.closeEntry()
        it.putNextEntry(ZipEntry("META-INF/services/javax.annotation.processing.Processor"))
        it.write(processors.joinToString("\n") { (_, procClass) ->
            procClass.name
        }.toByteArray())
        it.closeEntry()
    }
    return processorPath
}