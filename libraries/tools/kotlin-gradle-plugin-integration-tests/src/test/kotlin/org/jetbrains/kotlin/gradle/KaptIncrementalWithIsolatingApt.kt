/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.jetbrains.kotlin.gradle.incapt.IncrementalAggregatingReferencingClasspathProcessor
import org.jetbrains.kotlin.gradle.incapt.IncrementalBinaryIsolatingProcessor
import org.jetbrains.kotlin.gradle.incapt.IncrementalProcessor
import org.jetbrains.kotlin.gradle.incapt.IncrementalProcessorReferencingClasspath
import org.jetbrains.kotlin.gradle.util.AGPVersion
import org.jetbrains.kotlin.gradle.util.modify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    /** Regression test for https://youtrack.jetbrains.com/issue/KT-42182. */
    @Test
    fun testGeneratedSourcesImpactedByClasspathChanges() {
        val project = Project(
            "kaptIncrementalCompilationProject",
            GradleVersionRequired.None
        ).apply {
            setupIncrementalAptProject("ISOLATING", procClass = IncrementalProcessorReferencingClasspath::class.java)
        }
        project.gradleSettingsScript().writeText("include ':', ':lib'")
        val classpathTypeSource = project.projectDir.resolve("lib").run {
            mkdirs()
            resolve("build.gradle").writeText("apply plugin: 'java'")
            val source = resolve("src/main/java/" + IncrementalProcessorReferencingClasspath.CLASSPATH_TYPE.replace(".", "/") + ".java")
            source.parentFile.mkdirs()

            source.writeText(
                """
                package ${IncrementalProcessorReferencingClasspath.CLASSPATH_TYPE.substringBeforeLast(".")};
                public class ${IncrementalProcessorReferencingClasspath.CLASSPATH_TYPE.substringAfterLast(".")} {}
            """.trimIndent()
            )
            return@run source
        }
        project.gradleBuildScript().appendText(
            """
                
            dependencies {
                implementation project(':lib')
            }
        """.trimIndent()
        )

        val annotatedKotlinStubs = setOf(
            project.projectDir.resolve("build/tmp/kapt3/stubs/main/foo/A.java").canonicalPath,
            project.projectDir.resolve("build/tmp/kapt3/stubs/main/bar/B.java").canonicalPath,
            project.projectDir.resolve("build/tmp/kapt3/stubs/main/bar/UseBKt.java").canonicalPath,
            project.projectDir.resolve("build/tmp/kapt3/stubs/main/baz/UtilKt.java").canonicalPath,
            project.projectDir.resolve("build/tmp/kapt3/stubs/main/baz/UtilKt.java").canonicalPath,
            project.projectDir.resolve("build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath
        )

        val allKotlinStubs = annotatedKotlinStubs + setOf(
            project.projectDir.resolve("build/tmp/kapt3/stubs/main/delegate/Delegate.java").canonicalPath,
            project.projectDir.resolve("build/tmp/kapt3/stubs/main/delegate/Usage.java").canonicalPath
        )

        project.build("clean", "build") {
            assertSuccessful()
            assertEquals(allKotlinStubs + fileInWorkingDir("src/main/java/foo/JavaClass.java").canonicalPath, getProcessedSources(output))
        }

        // change type that all generated sources reference
        classpathTypeSource.writeText(classpathTypeSource.readText().replace("}", "int i = 10;\n}"))
        project.build("build") {
            assertSuccessful()
            assertEquals(annotatedKotlinStubs, getProcessedSources(output))
        }
    }

    /** Regression test for KT-34340. */
    @Test
    fun testIsolatingWithOriginsInClasspath() {
        //https://youtrack.jetbrains.com/issue/KTI-405
        if (System.getProperty("os.name")?.toLowerCase()?.contains("windows") == true) return

        val project = Project("kaptIncrementalWithParceler", GradleVersionRequired.None).apply {
            setupWorkingDir()
        }
        val options = defaultBuildOptions().copy(androidGradlePluginVersion = AGPVersion.v3_6_0)
        project.build("clean", ":mylibrary:assembleDebug", options = options) {
            assertSuccessful()
        }

        project.projectFile("BaseClassParcel.java").modify { current ->
            current.replace("protected FieldClassParcel", "private FieldClassParcel")
        }

        project.build(":mylibrary:assembleDebug", options = options) {
            assertSuccessful()
            assertEquals(
                setOf(
                    fileInWorkingDir("mylibrary/src/main/java/com/example/lib/ExampleParcel.java").canonicalPath,
                    fileInWorkingDir("baseLibrary/src/main/java/com/example/lib2/basemodule/BaseClassParcel.java").canonicalPath,
                ),
                getProcessedSources(output)
            )
        }
    }

    /**
     * Make sure that changes to classpath can cause types to be reprocessed (i.e types in generated .class files that contain annotations
     * claimed by annotation processors).
     */
    @Test
    fun testClasspathChangesCauseTypesToBeReprocessed() {
        val project = Project(
            "kaptIncrementalCompilationProject",
            GradleVersionRequired.None
        ).apply {
            setupIncrementalAptProject(
                Pair("ISOLATING", IncrementalBinaryIsolatingProcessor::class.java),
                Pair("AGGREGATING", IncrementalAggregatingReferencingClasspathProcessor::class.java),
            )
        }
        project.gradleSettingsScript().writeText("include ':', ':lib'")
        val classpathTypeSource = project.projectDir.resolve("lib").run {
            mkdirs()
            resolve("build.gradle").writeText("apply plugin: 'java'")
            val source =
                resolve("src/main/java/" + IncrementalAggregatingReferencingClasspathProcessor.CLASSPATH_TYPE.replace(".", "/") + ".java")
            source.parentFile.mkdirs()

            source.writeText(
                """
                package ${IncrementalAggregatingReferencingClasspathProcessor.CLASSPATH_TYPE.substringBeforeLast(".")};
                public class ${IncrementalAggregatingReferencingClasspathProcessor.CLASSPATH_TYPE.substringAfterLast(".")} {}
            """.trimIndent()
            )
            return@run source
        }
        project.gradleBuildScript().appendText(
            """
                
            dependencies {
                implementation project(':lib')
            }
        """.trimIndent()
        )

        // Remove all sources, and add only 1 source file
        project.projectDir.resolve("src").let {
            it.deleteRecursively()
            with(it.resolve("main/java/example/A.kt")) {
                parentFile.mkdirs()
                writeText(
                    """
                    package example
                    
                    annotation class ExampleAnnotation
                    @ExampleAnnotation
                    class A
                """.trimIndent()
                )
            }
        }

        val allKotlinStubs = setOf(
            project.projectDir.resolve("build/tmp/kapt3/stubs/main/example/ExampleAnnotation.java").canonicalPath,
            project.projectDir.resolve("build/tmp/kapt3/stubs/main/example/A.java").canonicalPath,
            project.projectDir.resolve("build/tmp/kapt3/stubs/main/error/NonExistentClass.java").canonicalPath
        )

        project.build("clean", "build") {
            assertSuccessful()
            assertEquals(allKotlinStubs, getProcessedSources(output))

            assertTrue(
                "Aggregating sources exists",
                fileInWorkingDir("build/generated/source/kapt/main/com/example/AggGenerated.java").exists()
            )
        }

        // change type that the aggregated generated source reference
        classpathTypeSource.writeText(classpathTypeSource.readText().replace("}", "int i = 10;\n}"))
        project.build("build") {
            assertSuccessful()
            assertEquals(emptySet<String>(), getProcessedSources(output))
            assertEquals(setOf("example.AGenerated"), getProcessedTypes(output))
            assertTrue(
                "Aggregating sources exists",
                fileInWorkingDir("build/generated/source/kapt/main/com/example/AggGenerated.java").exists()
            )
        }
    }
}

private const val patternApt = "Processing java sources with annotation processors:"
fun getProcessedSources(output: String): Set<String> {
    return output.lines().filter { it.contains(patternApt) }.flatMapTo(HashSet()) { logging ->
        val indexOf = logging.indexOf(patternApt) + patternApt.length
        logging.drop(indexOf).split(",").map { it.trim() }.filter { !it.isEmpty() }.toSet()
    }
}

private const val patternClassesApt = "Processing types with annotation processors: "
fun getProcessedTypes(output: String): Set<String> {
    return output.lines().filter { it.contains(patternClassesApt) }.flatMapTo(HashSet()) { logging ->
        val indexOf = logging.indexOf(patternClassesApt) + patternClassesApt.length
        logging.drop(indexOf).split(",").map { it.trim() }.filter { !it.isEmpty() }.toSet()
    }
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
