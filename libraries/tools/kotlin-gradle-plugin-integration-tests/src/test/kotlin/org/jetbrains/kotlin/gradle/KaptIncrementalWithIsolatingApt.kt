/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.api.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.incapt.IncrementalAggregatingReferencingClasspathProcessor
import org.jetbrains.kotlin.gradle.incapt.IncrementalBinaryIsolatingProcessor
import org.jetbrains.kotlin.gradle.incapt.IncrementalProcessor
import org.jetbrains.kotlin.gradle.incapt.IncrementalProcessorReferencingClasspath
import org.jetbrains.kotlin.gradle.testbase.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import test.kt33617.MyClass
import java.io.File
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.*
import kotlin.test.assertEquals

@DisplayName("Kapt incremental tests with isolating apt")
open class KaptIncrementalWithIsolatingApt : KaptIncrementalIT() {

    override val defaultBuildOptions = super.defaultBuildOptions.copy(
        incremental = true,
        kaptOptions = super.defaultBuildOptions.kaptOptions!!.copy(
            verbose = true,
            incrementalKapt = true,
            includeCompileClasspath = false
        )
    )

    override fun KGPBaseTest.kaptProject(
        gradleVersion: GradleVersion,
        buildOptions: BuildOptions,
        buildJdk: File?,
        test: TestProject.() -> Unit
    ): TestProject {
        return project(PROJECT_NAME, gradleVersion, buildOptions = buildOptions, buildJdk = buildJdk) {
            setupIncrementalAptProject("ISOLATING")

            test(this)
        }
    }

    @DisplayName("Incremental changes")
    @GradleTest
    fun testIncrementalChanges(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("clean", "assemble")

            javaSourcesDir().resolve("bar/useB.kt").modify { current -> "$current\nfun otherFunction() {}" }
            build("assemble") {
                assertEquals(
                    setOf(
                        "$KAPT3_STUBS_PATH/bar/UseBKt.java",
                        "$KAPT3_STUBS_PATH/error/NonExistentClass.java"
                    )
                        .map { projectPath.resolve(it).toRealPath().toString() }
                        .toSet(),
                    getProcessedSources(output)
                )
            }

            javaSourcesDir().resolve("bar/B.kt").modify { current ->
                val lastBrace = current.lastIndexOf("}")
                current.substring(0, lastBrace) + "fun anotherFun() {}\n }"
            }
            build("assemble") {
                assertEquals(
                    setOf(
                        "$KAPT3_STUBS_PATH/bar/B.java",
                        "$KAPT3_STUBS_PATH/bar/UseBKt.java",
                        "$KAPT3_STUBS_PATH/error/NonExistentClass.java",
                    )
                        .map { projectPath.resolve(it).toRealPath().toString() }
                        .toSet(),
                    getProcessedSources(output)
                )
            }
        }
    }

    @DisplayName("On changing annotation processor classpath")
    @GradleTest
    fun testChangingAnnotationProcessorClasspath(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("clean", "assemble")

            buildGradle.append(
                """
    
                    dependencies {
                        kapt 'com.google.guava:guava:12.0'
                    }
                    """.trimIndent()
            )

            build("assemble") {
                assertOutputContains("The input changes require a full rebuild for incremental task ':kaptKotlin'.")
            }
        }
    }

    @DisplayName("On unchanged annotation processor classpath, but processor itself is updated")
    @GradleTest
    fun testUnchangedAnnotationProcessorClasspathButContentChanged(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            val processorJar = projectPath.resolve("processor.jar").also {
                ZipOutputStream(it.outputStream()).use {
                    // create an empty jar
                }
            }

            buildGradle.append(
                """
    
                    dependencies {
                        kapt files("processor.jar")
                    }
                    """.trimIndent()
            )

            build("clean", "assemble")

            ZipOutputStream(processorJar.outputStream()).use {
                it.putNextEntry(ZipEntry("resource.txt"))
                it.closeEntry()
            }

            build("assemble") {
                assertOutputContains("The input changes require a full rebuild for incremental task ':kaptKotlin'.")
            }
        }
    }

    @DisplayName("Runs non-incrementally on unrecognized inputs")
    @GradleTest
    fun testNonIncrementalWithUnrecognizedInputs(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            val additionalInputs = projectPath.resolve("additionalInputs").also { it.createDirectories() }
            buildGradle.appendText(
                """
    
                    tasks.whenTaskAdded {
                        if (it.name == "kaptKotlin") {
                          it.getInputs().files("${additionalInputs.invariantSeparatorsPathString}")
                        }
                    }
                    """.trimIndent()
            )

            build("clean", "assemble")

            additionalInputs.resolve("layout.xml").createFile()
            build("assemble") {
                assertOutputContains("Incremental annotation processing (apt mode): false")
            }
        }
    }

    @DisplayName("KT-33617: sources in compile classpath jars")
    @JdkVersions(versions = [JavaVersion.VERSION_11])
    @GradleWithJdkTest
    fun testSourcesInCompileClasspathJars(
        gradleVersion: GradleVersion,
        jdk: JdkVersions.ProvidedJdk
    ) {
        kaptProject(gradleVersion, buildJdk = jdk.location) {
            // create jar with .class and .java file for the same type
            ZipOutputStream(projectPath.resolve("lib-with-sources.jar").outputStream()).use {
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

            buildGradle.append(
                """
    
                    dependencies {
                        implementation files('lib-with-sources.jar')
                    }
                    """.trimIndent()
            )
            javaSourcesDir().resolve("bar/useB.kt").modify { current ->
                "$current\nfun otherFunction(param: test.kt33617.MyClass) {}"
            }

            build("clean", "kaptKotlin")
        }
    }

    @DisplayName("KT-42182: generated sources are updated on classpath changes")
    @GradleTest
    fun testGeneratedSourcesImpactedByClasspathChanges(gradleVersion: GradleVersion) {
        project(PROJECT_NAME, gradleVersion) {
            setupIncrementalAptProject(
                "ISOLATING",
                procClass = IncrementalProcessorReferencingClasspath::class.java
            )

            settingsGradle.append("\ninclude ':', ':lib'")

            val classpathTypeSource = subProject("lib").run {
                projectPath.createDirectory()
                buildGradle.writeText(
                    """
                    plugins {
                        id 'java'
                    }
                    """.trimIndent()
                )
                val source = javaSourcesDir()
                    .resolve(
                        IncrementalProcessorReferencingClasspath.CLASSPATH_TYPE
                            .replace(".", "/") + ".java"
                    )
                source.parent.createDirectories()

                source.writeText(
                    """
                    package ${IncrementalProcessorReferencingClasspath.CLASSPATH_TYPE.substringBeforeLast(".")};
                    public class ${IncrementalProcessorReferencingClasspath.CLASSPATH_TYPE.substringAfterLast(".")} {}
                    """.trimIndent()
                )
                return@run source
            }

            buildGradle.appendText(
                """

                dependencies {
                    implementation project(':lib')
                }
                """.trimIndent()
            )

            val annotatedKotlinStubs = listOf(
                "$KAPT3_STUBS_PATH/foo/A.java",
                "$KAPT3_STUBS_PATH/bar/B.java",
                "$KAPT3_STUBS_PATH/bar/UseBKt.java",
                "$KAPT3_STUBS_PATH/baz/UtilKt.java",
                "$KAPT3_STUBS_PATH/baz/UtilKt.java",
                "$KAPT3_STUBS_PATH/error/NonExistentClass.java"
            )

            val allKotlinStubs = annotatedKotlinStubs + listOf(
                "$KAPT3_STUBS_PATH/delegate/Delegate.java",
                "$KAPT3_STUBS_PATH/delegate/Usage.java"
            )

            build("clean", "assemble") {
                assertEquals(
                    allKotlinStubs
                        .plus("src/main/java/foo/JavaClass.java")
                        .map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )
            }

            // change type that all generated sources reference
            classpathTypeSource.writeText(
                classpathTypeSource.readText().replace("}", "int i = 10;\n}")
            )
            build("assemble") {
                assertEquals(
                    annotatedKotlinStubs.map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )
            }
        }
    }

    @DisplayName("KT-34340: origins in classpath")
    @GradleAndroidTest
    @DisabledOnOs(OS.WINDOWS, disabledReason = "https://youtrack.jetbrains.com/issue/KTI-405")
    fun testIsolatingWithOriginsInClasspath(
        gradleVersion: GradleVersion,
        agpVersion: String,
        providedJdk: JdkVersions.ProvidedJdk
    ) {
        project(
            "kaptIncrementalWithParceler",
            gradleVersion,
            buildOptions = defaultBuildOptions.copy(androidVersion = agpVersion),
            buildJdk = providedJdk.location
        ) {
            // Remove the once minimal supported AGP version will be 8.1.0: https://issuetracker.google.com/issues/260059413
            gradleProperties.appendText(
                """
                |kotlin.jvm.target.validation.mode=warning
                """.trimMargin()
            )

            build("clean", ":mylibrary:assembleDebug")

            subProject("baseLibrary")
                .javaSourcesDir()
                .resolve("com/example/lib2/basemodule/BaseClassParcel.java")
                .modify { current ->
                    current.replace("protected FieldClassParcel", "private FieldClassParcel")
                }

            build(":mylibrary:assembleDebug") {
                assertEquals(
                    listOf(
                        "baseLibrary/build/tmp/kapt3/stubs/debug/error/NonExistentClass.java",
                        "mylibrary/src/main/java/com/example/lib/ExampleParcel.java",
                        "baseLibrary/src/main/java/com/example/lib2/basemodule/BaseClassParcel.java",
                    ).map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )
            }
        }
    }

    /**
     * Make sure that changes to classpath can cause types to be reprocessed (i.e types in generated .class files that contain annotations
     * claimed by annotation processors).
     */
    @DisplayName("Changed types causing incremental compilation")
    @GradleTest
    fun testClasspathChangesCauseTypesToBeReprocessed(gradleVersion: GradleVersion) {
        project(PROJECT_NAME, gradleVersion) {
            setupIncrementalAptProject(
                Pair("ISOLATING", IncrementalBinaryIsolatingProcessor::class.java),
                Pair("AGGREGATING", IncrementalAggregatingReferencingClasspathProcessor::class.java),
            )

            settingsGradle.append("\ninclude ':', ':lib'\n")
            val classpathTypeSource = subProject("lib").run {
                projectPath.createDirectories()

                buildGradle.writeText(
                    """
                    plugins {
                        id 'java'
                    }
                    
                    """.trimIndent()
                )
                val source = javaSourcesDir()
                    .resolve(
                        IncrementalAggregatingReferencingClasspathProcessor.CLASSPATH_TYPE
                            .replace(".", "/") + ".java"
                    )
                source.parent.createDirectories()

                source.writeText(
                    """
                    package ${IncrementalAggregatingReferencingClasspathProcessor.CLASSPATH_TYPE.substringBeforeLast(".")};
                    public class ${IncrementalAggregatingReferencingClasspathProcessor.CLASSPATH_TYPE.substringAfterLast(".")} {}
                    """.trimIndent()
                )
                return@run source
            }

            buildGradle.append(
                """

                dependencies {
                    implementation project(':lib')
                }
                """.trimIndent()
            )

            // Remove all sources, and add only 1 source file
            javaSourcesDir().deleteRecursively()
            with(javaSourcesDir().resolve("example/A.kt")) {
                parent.createDirectories()
                writeText(
                    """
                    package example
                    
                    annotation class ExampleAnnotation
                    @ExampleAnnotation
                    class A
                    """.trimIndent()
                )
            }

            val allKotlinStubs = setOf(
                "build/tmp/kapt3/stubs/main/example/ExampleAnnotation.java",
                "build/tmp/kapt3/stubs/main/example/A.java",
                "build/tmp/kapt3/stubs/main/error/NonExistentClass.java"
            )

            build("clean", "assemble") {
                assertEquals(
                    allKotlinStubs.map { projectPath.resolve(it).toRealPath().toString() }.toSet(),
                    getProcessedSources(output)
                )
                assertFileInProjectExists("build/generated/source/kapt/main/com/example/AggGenerated.java")
            }

            // change type that the aggregated generated source reference
            classpathTypeSource.writeText(classpathTypeSource.readText().replace("}", "int i = 10;\n}"))
            build("assemble") {
                assertEquals(emptySet(), getProcessedSources(output))
                assertEquals(setOf("example.AGenerated"), getProcessedTypes(output))
                assertFileInProjectExists("build/generated/source/kapt/main/com/example/AggGenerated.java")
            }
        }
    }

    @DisplayName("KT-41456: detect missing kotlin compilation output")
    @GradleTest
    @DisabledOnOs(OS.WINDOWS, disabledReason = "https://youtrack.jetbrains.com/issue/KTI-405")
    fun testMissingKotlinOutputForcesNonIncrementalRun(gradleVersion: GradleVersion) {
        kaptProject(gradleVersion) {
            build("clean", "assemble")

            // Explicitly remove all kotlinc generated .class files
            projectPath.resolve("build/classes/kotlin/main").toFile().listFiles()!!.forEach { it.deleteRecursively() }
            javaSourcesDir().resolve("bar/useB.kt").modify { current -> "$current\nfun otherFunction() {}" }

            build("assemble") {
                assertOutputContains("Unable to run incrementally, processing all sources.")
            }
        }
    }
}

@DisplayName("Kapt incremental tests with isolating apt with precise compilation outputs backup")
class KaptIncrementalWithIsolatingAptAndPreciseBackup : KaptIncrementalWithIsolatingApt() {
    override val defaultBuildOptions = super.defaultBuildOptions.copy(usePreciseOutputsBackup = true, keepIncrementalCompilationCachesInMemory = true)
}

private const val patternApt = "Processing java sources with annotation processors:"
fun getProcessedSources(output: String): Set<String> {
    return output.lines().filter { it.contains(patternApt) }.flatMapTo(HashSet()) { logging ->
        val indexOf = logging.indexOf(patternApt) + patternApt.length
        logging.drop(indexOf).split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}

private const val patternClassesApt = "Processing types with annotation processors: "
fun getProcessedTypes(output: String): Set<String> {
    return output.lines().filter { it.contains(patternClassesApt) }.flatMapTo(HashSet()) { logging ->
        val indexOf = logging.indexOf(patternClassesApt) + patternClassesApt.length
        logging.drop(indexOf).split(",").map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }
}

fun TestProject.setupIncrementalAptProject(
    procType: String,
    buildFile: Path = buildGradle,
    procClass: Class<*> = IncrementalProcessor::class.java
) {
    setupIncrementalAptProject(procType to procClass, buildFile = buildFile)
}

@OptIn(ExperimentalPathApi::class)
fun TestProject.setupIncrementalAptProject(
    vararg processors: Pair<String, Class<*>>,
    buildFile: Path = buildGradle
) {
    val content = buildFile.readText()
    val processorPath = generateProcessor(*processors)

    val updatedContent = content.replace(
        Regex("^\\s*kapt\\s\"org\\.jetbrains\\.kotlin.*$", RegexOption.MULTILINE),
        "    kapt files(\"${processorPath.invariantSeparatorsPath}\")"
    )
    buildFile.writeText(updatedContent)
}

fun TestProject.generateProcessor(
    vararg processors: Pair<String, Class<*>>
): File {
    val processorPath = projectPath.resolve("incrementalProcessor.jar").toFile()

    ZipOutputStream(processorPath.outputStream()).use {
        for ((_, procClass) in processors) {
            val path = procClass.name.replace(".", "/") + ".class"
            procClass.classLoader.getResourceAsStream(path).use { inputStream ->
                it.putNextEntry(ZipEntry(path))
                it.write(inputStream!!.readBytes())
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
