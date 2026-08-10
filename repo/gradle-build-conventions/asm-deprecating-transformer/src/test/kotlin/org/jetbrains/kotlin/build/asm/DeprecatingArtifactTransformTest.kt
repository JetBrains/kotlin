/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build.asm

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeprecatingArtifactTransformTest {
    @TempDir
    lateinit var projectDir: File

    @Test
    fun `transforms matching classes and preserves excluded classes and resources`() {
        writeProject(
            pattern = "org.example.**",
            exclusions = listOf("org.example.api.**"),
            classes = listOf("org/example/Tree", "org/example/api/Fruit", "com/example/App"),
        )

        val result = runGradle("resolveDeprecated", "--build-cache")

        assertEquals(TaskOutcome.SUCCESS, result.task(":resolveDeprecated")?.outcome)
        val outputJar = projectDir.resolve("build/resolved/input.jar")
        ZipFile(outputJar).use { output ->
            assertDeprecated(output, "org/example/Tree.class", expected = true)
            assertDeprecated(output, "org/example/api/Fruit.class", expected = false)
            assertDeprecated(output, "com/example/App.class", expected = false)
            assertEquals("resource contents", output.getInputStream(output.getEntry("resource.txt")).reader().readText())
        }
        val firstOutput = outputJar.readBytes()

        projectDir.resolve("build").deleteRecursively()
        val cachedResult = runGradle("resolveDeprecated", "--build-cache")

        assertEquals(TaskOutcome.SUCCESS, cachedResult.task(":resolveDeprecated")?.outcome)
        assertContentEquals(firstOutput, outputJar.readBytes())
    }

    @Test
    fun `rejects classes in the unnamed package`() {
        writeProject(
            pattern = "Root",
            exclusions = emptyList(),
            classes = listOf("Root"),
        )

        val result = runGradleAndFail("resolveDeprecated")

        assertTrue(result.output.contains("Deprecating classes in the default (unnamed) package is not supported"))
    }

    @Test
    fun `transforms jar produced by a project dependency on a clean build`() {
        projectDir.resolve("settings.gradle.kts").writeText(
            """
            rootProject.name = "artifact-transform-project-dependency-test"
            include(":producer")
            """.trimIndent()
        )
        projectDir.resolve("build.gradle.kts").writeText(
            """
            import org.gradle.api.attributes.LibraryElements
            import org.gradle.api.attributes.Usage
            import org.gradle.jvm.tasks.Jar

            plugins {
                id("asm-deprecating-transformer")
            }

            val embedded = configurations.create("embedded") {
                isCanBeConsumed = false
                isCanBeResolved = true
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
                }
            }

            dependencies {
                embedded(project(":producer"))
            }

            asmDeprecation {
                deprecateClassesByPattern(
                    inputConfigurations = listOf(embedded),
                    pattern = "org.example.**",
                    deprecationMessage = "Deprecated for testing",
                )
            }

            tasks.register<Jar>("resolveDeprecated") {
                from(embedded.elements.map { dependencies ->
                    dependencies.map { zipTree(it) }
                })
                archiveFileName = "output.jar"
                destinationDirectory = layout.buildDirectory.dir("resolved")
            }
            """.trimIndent()
        )
        projectDir.resolve("producer/build.gradle.kts").apply {
            parentFile.mkdirs()
            writeText(
                """
                plugins {
                    `java-library`
                }

                tasks.jar {
                    from("content")
                }
                """.trimIndent()
            )
        }
        projectDir.resolve("producer/content/org/example/Tree.class").apply {
            parentFile.mkdirs()
            writeBytes(createClass("org/example/Tree"))
        }

        val result = runGradle("resolveDeprecated")

        assertEquals(TaskOutcome.SUCCESS, result.task(":producer:jar")?.outcome)
        ZipFile(projectDir.resolve("build/resolved/output.jar")).use { output ->
            assertDeprecated(output, "org/example/Tree.class", expected = true)
        }
    }

    private fun writeProject(pattern: String, exclusions: List<String>, classes: List<String>) {
        projectDir.resolve("settings.gradle.kts").writeText("rootProject.name = \"artifact-transform-test\"")
        projectDir.resolve("build.gradle.kts").writeText(
            """
            import org.gradle.api.attributes.LibraryElements
            import org.gradle.api.attributes.Usage

            plugins {
                id("asm-deprecating-transformer")
            }

            val embedded by configurations.creating {
                isCanBeConsumed = false
                isCanBeResolved = true
                attributes {
                    attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
                    attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
                }
            }

            dependencies {
                embedded(files("input.jar"))
            }

            asmDeprecation {
                deprecateClassesByPattern(
                    inputConfigurations = listOf(embedded),
                    pattern = "$pattern",
                    deprecationMessage = "Deprecated for testing",
                    exclusions = listOf(${exclusions.joinToString { "\"$it\"" }}),
                )
            }

            tasks.register<Copy>("resolveDeprecated") {
                from(embedded)
                into(layout.buildDirectory.dir("resolved"))
            }
            """.trimIndent()
        )
        ZipOutputStream(projectDir.resolve("input.jar").outputStream()).use { output ->
            classes.forEach { className ->
                output.putNextEntry(ZipEntry("$className.class"))
                output.write(createClass(className))
                output.closeEntry()
            }
            output.putNextEntry(ZipEntry("resource.txt"))
            output.write("resource contents".toByteArray())
            output.closeEntry()
        }
    }

    private fun runGradle(vararg arguments: String) =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*arguments, "--stacktrace")
            .build()

    private fun runGradleAndFail(vararg arguments: String) =
        GradleRunner.create()
            .withProjectDir(projectDir)
            .withPluginClasspath()
            .withArguments(*arguments, "--stacktrace")
            .buildAndFail()

    private fun createClass(className: String): ByteArray {
        val writer = ClassWriter(0)
        writer.visit(Opcodes.V1_8, Opcodes.ACC_PUBLIC, className, null, "java/lang/Object", null)
        writer.visitEnd()
        return writer.toByteArray()
    }

    private fun assertDeprecated(zipFile: ZipFile, path: String, expected: Boolean) {
        var deprecatedAnnotation = false
        var deprecatedAccess = false
        ClassReader(zipFile.getInputStream(zipFile.getEntry(path))).accept(
            object : ClassVisitor(Opcodes.API_VERSION) {
                override fun visit(
                    version: Int,
                    access: Int,
                    name: String,
                    signature: String?,
                    superName: String?,
                    interfaces: Array<out String>?,
                ) {
                    deprecatedAccess = access and Opcodes.ACC_DEPRECATED != 0
                }

                override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
                    if (descriptor == "Lkotlin/Deprecated;") {
                        deprecatedAnnotation = true
                    }
                    return null
                }
            },
            0,
        )
        if (expected) {
            assertTrue(deprecatedAccess, "$path does not have ACC_DEPRECATED")
            assertTrue(deprecatedAnnotation, "$path does not have kotlin.Deprecated")
        } else {
            assertFalse(deprecatedAccess, "$path unexpectedly has ACC_DEPRECATED")
            assertFalse(deprecatedAnnotation, "$path unexpectedly has kotlin.Deprecated")
        }
    }
}